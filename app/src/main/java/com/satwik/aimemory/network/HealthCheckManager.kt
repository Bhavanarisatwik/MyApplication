package com.satwik.aimemory.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Periodically polls GET /health to monitor OpenClaw backend connectivity.
 *
 * - Polls every 30s when connected
 * - Uses exponential backoff on failure (30s → 60s → 120s → max 5min)
 * - Exposes isConnected StateFlow for UI observation
 */
object HealthCheckManager {

    private const val TAG = "HealthCheckManager"
    private const val BASE_INTERVAL_MS = 30_000L
    private const val MAX_INTERVAL_MS = 300_000L // 5 minutes
    private const val BACKOFF_MULTIPLIER = 2.0

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastCheckTimestamp = MutableStateFlow(0L)
    val lastCheckTimestamp: StateFlow<Long> = _lastCheckTimestamp.asStateFlow()

    private var pollingJob: Job? = null
    private var pollingScope: CoroutineScope? = null
    private var currentIntervalMs = BASE_INTERVAL_MS
    private var consecutiveFailures = 0

    /**
     * Start periodic health checking. Safe to call multiple times;
     * subsequent calls are no-ops if already running.
     */
    fun start() {
        if (pollingJob?.isActive == true) return

        pollingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        pollingJob = pollingScope?.launch {
            Log.d(TAG, "Health check polling started")

            // Do an immediate check on start
            performCheck()

            while (isActive) {
                delay(currentIntervalMs)
                performCheck()
            }
        }
    }

    /**
     * Stop health checking (e.g., when app goes to background).
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        pollingScope?.cancel()
        pollingScope = null
        Log.d(TAG, "Health check polling stopped")
    }

    /**
     * Perform a single health check immediately.
     * Can be called manually to force a connectivity re-check.
     */
    suspend fun checkNow(): Boolean {
        return performCheck()
    }

    private suspend fun performCheck(): Boolean {
        return try {
            val response = NetworkModule.api.healthCheck()
            _lastCheckTimestamp.value = System.currentTimeMillis()

            if (response.isSuccessful) {
                onSuccess()
                true
            } else {
                onFailure("HTTP ${response.code()}: ${response.message()}")
                false
            }
        } catch (e: CancellationException) {
            throw e // Don't catch coroutine cancellation
        } catch (e: Exception) {
            _lastCheckTimestamp.value = System.currentTimeMillis()
            onFailure(e.message ?: "Unknown error")
            false
        }
    }

    private fun onSuccess() {
        if (!_isConnected.value) {
            Log.d(TAG, "Backend connected ✓")
        }
        _isConnected.value = true
        consecutiveFailures = 0
        currentIntervalMs = BASE_INTERVAL_MS
    }

    private fun onFailure(reason: String) {
        consecutiveFailures++
        _isConnected.value = false

        // Exponential backoff
        currentIntervalMs = (BASE_INTERVAL_MS * Math.pow(BACKOFF_MULTIPLIER, 
            consecutiveFailures.coerceAtMost(6).toDouble())).toLong()
            .coerceAtMost(MAX_INTERVAL_MS)

        Log.d(TAG, "Health check failed ($reason). " +
              "Failures: $consecutiveFailures, next check in ${currentIntervalMs / 1000}s")
    }
}
