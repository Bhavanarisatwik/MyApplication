package com.satwik.aimemory.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Periodically polls GET /summary/latest to retrieve structured summaries.
 *
 * Behavior:
 * - Polls every [POLL_INTERVAL_MS] when backend is reachable
 * - Exposes the latest summary via StateFlow
 * - Stops polling when backend goes unreachable
 * - HTTP 204 (no content) is treated as a valid empty response
 */
object SummaryPoller {

    private const val TAG = "SummaryPoller"
    private const val POLL_INTERVAL_MS = 60_000L  // 1 minute between polls

    private var pollerJob: Job? = null

    private val _latestSummary = MutableStateFlow<SummaryResponse?>(null)
    val latestSummary: StateFlow<SummaryResponse?> = _latestSummary.asStateFlow()

    private val _lastPollTimestamp = MutableStateFlow(0L)
    val lastPollTimestamp: StateFlow<Long> = _lastPollTimestamp.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    /**
     * Start the summary poller.
     */
    fun start() {
        if (pollerJob?.isActive == true) return

        pollerJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            Log.d(TAG, "Summary poller started")

            while (isActive) {
                if (ApiGateway.canMakeApiCalls()) {
                    poll()
                }
                delay(POLL_INTERVAL_MS)
            }

            Log.d(TAG, "Summary poller stopped")
        }
    }

    /**
     * Force an immediate poll.
     */
    suspend fun pollNow() {
        poll()
    }

    /**
     * Stop the summary poller.
     */
    fun stop() {
        pollerJob?.cancel()
        pollerJob = null
        _isPolling.value = false
        Log.d(TAG, "Summary poller stopped")
    }

    private suspend fun poll() {
        _isPolling.value = true
        try {
            // Route through ApiGateway to ensure auth header is always attached
            val summary = ApiGateway.getLatestSummary()
            _lastPollTimestamp.value = System.currentTimeMillis()

            if (summary != null) {
                _latestSummary.value = summary
                Log.d(TAG, "Got latest summary: ${summary.rawSnippet?.take(60)}")
            } else {
                // null means either HTTP 204, backend unreachable, or auth failure
                // ApiGateway already logged the specific reason
                Log.d(TAG, "No summary available (null response from gateway)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Summary poll error", e)
        } finally {
            _isPolling.value = false
        }
    }
}
