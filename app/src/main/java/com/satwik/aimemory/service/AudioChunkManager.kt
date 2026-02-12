package com.satwik.aimemory.service

import android.util.Log
import java.time.LocalDateTime

/**
 * Manages PCM audio accumulation during speech segments.
 * Handles dynamic chunking: accumulates samples while speech is detected,
 * finalizes when silence is sustained or max duration is reached.
 */
class AudioChunkManager(
    private val maxChunkDurationSeconds: Float = 30f,
    private val sampleRate: Int = 16000
) {
    companion object {
        private const val TAG = "AudioChunkManager"
    }

    private val pcmBuffer = mutableListOf<Short>()
    private var chunkStartTime: LocalDateTime? = null
    private var isCapturing = false

    private val maxSamples: Int = (maxChunkDurationSeconds * sampleRate).toInt()

    /**
     * Begin a new speech chunk. Called when VAD first detects speech.
     */
    fun startChunk() {
        if (isCapturing) {
            Log.w(TAG, "[CHUNK] startChunk() called but already capturing (bufferSize=${pcmBuffer.size})")
            return
        }
        pcmBuffer.clear()
        chunkStartTime = LocalDateTime.now()
        isCapturing = true
        Log.w(TAG, "[CHUNK] ▶ Started new audio chunk at $chunkStartTime")
    }

    /**
     * Append PCM samples to the current chunk.
     * Returns a finalized AudioChunk if max duration is reached (force split).
     */
    fun appendSamples(samples: ShortArray): AudioChunk? {
        if (!isCapturing) {
            Log.w(TAG, "[CHUNK] appendSamples called but NOT capturing, ignoring ${samples.size} samples")
            return null
        }

        pcmBuffer.addAll(samples.toList())

        // Force split if we've exceeded max duration
        if (pcmBuffer.size >= maxSamples) {
            Log.w(TAG, "[CHUNK] ⏫ Max chunk duration reached (${maxChunkDurationSeconds}s, ${pcmBuffer.size}/$maxSamples samples), force splitting")
            return finalizeChunk()
        }
        return null
    }

    /**
     * Finalize the current chunk (e.g., after sustained silence).
     * Returns the encoded AudioChunk, or null if nothing was captured.
     */
    fun finalizeChunk(): AudioChunk? {
        if (!isCapturing || pcmBuffer.isEmpty()) {
            Log.w(TAG, "[CHUNK] finalizeChunk() called but isCapturing=$isCapturing, bufferSize=${pcmBuffer.size} — returning null")
            reset()
            return null
        }

        val pcmArray = pcmBuffer.toShortArray()
        val wavData = WavEncoder.encode(pcmArray)
        val duration = WavEncoder.durationSeconds(pcmArray.size)
        val startTime = chunkStartTime ?: LocalDateTime.now()

        Log.w(TAG, "[CHUNK] ✅ Finalized chunk: ${pcmArray.size} samples, ${duration}s, ${wavData.size} bytes WAV")

        val chunk = AudioChunk(
            id = "chunk_${System.currentTimeMillis()}",
            wavData = wavData,
            timestamp = startTime,
            durationSeconds = duration,
            sampleCount = pcmArray.size
        )

        reset()
        return chunk
    }

    /**
     * Discard current chunk without finalizing.
     */
    fun reset() {
        pcmBuffer.clear()
        chunkStartTime = null
        isCapturing = false
    }

    /**
     * Whether we are currently accumulating audio samples.
     */
    fun isCurrentlyCapturing(): Boolean = isCapturing

    /**
     * Current chunk duration in seconds.
     */
    fun currentDurationSeconds(): Float {
        return if (isCapturing) pcmBuffer.size.toFloat() / sampleRate else 0f
    }
}

/**
 * Represents a finalized audio chunk ready for upload.
 */
data class AudioChunk(
    val id: String,
    val wavData: ByteArray,
    val timestamp: LocalDateTime,
    val durationSeconds: Float,
    val sampleCount: Int,
    var retryCount: Int = 0,
    var status: ChunkStatus = ChunkStatus.PENDING
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioChunk) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class ChunkStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}
