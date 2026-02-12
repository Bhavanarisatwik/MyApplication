package com.satwik.aimemory.network

import android.util.Log
import com.satwik.aimemory.service.AudioChunkQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Drains the AudioChunkQueue and uploads WAV files to OpenClaw.
 *
 * Behavior:
 * - Polls the queue every [POLL_INTERVAL_MS] when backend is reachable
 * - Dequeues a chunk, uploads via ApiGateway, marks uploaded on success
 * - On failure, re-enqueues the chunk and backs off
 * - Respects health-gate: stops uploading if backend goes unreachable
 */
object ChunkUploadWorker {

    private const val TAG = "ChunkUploadWorker"
    private const val POLL_INTERVAL_MS = 5_000L       // 5 seconds between drain attempts
    private const val FAILURE_BACKOFF_MS = 15_000L    // 15 seconds on upload failure
    private const val MAX_RETRIES_PER_CHUNK = 3

    private var workerJob: Job? = null
    private var chunkQueue: AudioChunkQueue? = null

    private val _uploadedCount = MutableStateFlow(0)
    val uploadedCount: StateFlow<Int> = _uploadedCount.asStateFlow()

    private val _failedCount = MutableStateFlow(0)
    val failedCount: StateFlow<Int> = _failedCount.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _lastUploadTimestamp = MutableStateFlow(0L)
    val lastUploadTimestamp: StateFlow<Long> = _lastUploadTimestamp.asStateFlow()

    private val _lastUploadedChunkId = MutableStateFlow<String?>(null)
    val lastUploadedChunkId: StateFlow<String?> = _lastUploadedChunkId.asStateFlow()

    /**
     * Start the upload worker. Attaches to the given chunk queue.
     */
    fun start(queue: AudioChunkQueue) {
        if (workerJob?.isActive == true) return

        chunkQueue = queue
        workerJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            Log.d(TAG, "Upload worker started")

            while (isActive) {
                if (!ApiGateway.canMakeApiCalls()) {
                    // Backend unreachable — wait and retry
                    delay(POLL_INTERVAL_MS)
                    continue
                }

                val chunk = chunkQueue?.peek()
                if (chunk == null) {
                    // Queue empty — idle
                    delay(POLL_INTERVAL_MS)
                    continue
                }

                // Attempt upload
                _isUploading.value = true
                Log.d(TAG, "Uploading chunk ${chunk.id} (${chunk.wavData.size} bytes)")

                // Create multipart parts matching FastAPI endpoint:
                //   audio: UploadFile = File(...)
                //   timestamp: str = Form(...)
                //   device_id: str = Form(...)
                val audioPart = MultipartBody.Part.createFormData(
                    "audio",
                    "${chunk.id}.wav",
                    chunk.wavData.toRequestBody("audio/wav".toMediaType())
                )
                val timestampPart = chunk.timestamp.toString()
                    .toRequestBody("text/plain".toMediaType())
                val deviceIdPart = android.os.Build.MODEL
                    .toRequestBody("text/plain".toMediaType())

                val response = ApiGateway.uploadAudio(audioPart, timestampPart, deviceIdPart)
                _isUploading.value = false

                if (response != null && response.success) {
                    // Success — dequeue and clean up
                    chunkQueue?.dequeue()
                    chunkQueue?.markUploaded(chunk.id)
                    _uploadedCount.value++
                    _lastUploadTimestamp.value = System.currentTimeMillis()
                    _lastUploadedChunkId.value = chunk.id

                    Log.d(TAG, "Chunk ${chunk.id} uploaded successfully. " +
                            "Transcript: ${response.transcript?.take(50) ?: "none"}")

                    // Small delay between successful uploads to avoid flooding
                    delay(500)
                } else {
                    // Failed — back off
                    _failedCount.value++
                    Log.w(TAG, "Chunk ${chunk.id} upload failed, backing off ${FAILURE_BACKOFF_MS}ms")
                    delay(FAILURE_BACKOFF_MS)
                }
            }

            Log.d(TAG, "Upload worker stopped")
        }
    }

    /**
     * Stop the upload worker.
     */
    fun stop() {
        workerJob?.cancel()
        workerJob = null
        _isUploading.value = false
        Log.d(TAG, "Upload worker stopped")
    }
}
