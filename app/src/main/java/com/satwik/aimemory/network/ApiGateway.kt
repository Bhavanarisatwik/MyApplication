package com.satwik.aimemory.network

import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MultipartBody
import okhttp3.RequestBody

/**
 * Gates all OpenClaw API calls behind health check status.
 *
 * Rules:
 * - POST /audio/upload: BLOCKED until health check passes
 * - GET /summary/latest: BLOCKED until health check passes
 * - GET /health: Always allowed (used by HealthCheckManager)
 *
 * Callers should check [isBackendReachable] before attempting operations,
 * or use the gated wrapper methods which return null/error when unavailable.
 */
object ApiGateway {

    private const val TAG = "ApiGateway"

    /** Real-time backend connectivity status. */
    val isBackendReachable: StateFlow<Boolean> = HealthCheckManager.isConnected

    /**
     * Upload an audio chunk — only if backend is reachable.
     * @return UploadResponse on success, null if backend unreachable or request failed
     */
    suspend fun uploadAudio(
        audioPart: MultipartBody.Part,
        timestamp: RequestBody,
        deviceId: RequestBody
    ): UploadResponse? {
        if (!isBackendReachable.value) {
            Log.d(TAG, "Upload blocked: backend not reachable")
            return null
        }

        return try {
            val response = NetworkModule.api.uploadAudio(audioPart, timestamp, deviceId)
            if (response.isSuccessful) {
                Log.d(TAG, "Upload successful: ${response.body()?.chunkId}")
                response.body()
            } else {
                Log.w(TAG, "Upload failed: HTTP ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            null
        }
    }

    /**
     * Fetch the latest summary — only if backend is reachable.
     * @return SummaryResponse on success, null if unavailable
     */
    suspend fun getLatestSummary(): SummaryResponse? {
        if (!isBackendReachable.value) {
            Log.d(TAG, "Summary fetch blocked: backend not reachable")
            return null
        }

        return try {
            val response = NetworkModule.api.getLatestSummary()
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "Summary fetch failed: HTTP ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Summary fetch error", e)
            null
        }
    }

    /**
     * Submit a natural language query — only if backend is reachable.
     * @return QueryResponse on success, null if unavailable
     */
    suspend fun submitQuery(query: String): QueryResponse? {
        if (!isBackendReachable.value) {
            Log.d(TAG, "Query blocked: backend not reachable")
            return null
        }

        return try {
            val response = NetworkModule.api.submitQuery(QueryRequest(query))
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "Query failed: HTTP ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Query error", e)
            null
        }
    }

    /**
     * Check if the gateway would allow API calls right now.
     * Use this to show appropriate UI state (e.g., "Waiting for backend...").
     */
    fun canMakeApiCalls(): Boolean = isBackendReachable.value
}
