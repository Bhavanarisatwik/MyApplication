package com.satwik.aimemory.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe queue for pending audio chunks.
 * Persists WAV files to internal storage for durability across process restarts.
 * Will be consumed by the OpenClaw upload service in Phase 3.
 */
class AudioChunkQueue(private val context: Context) {

    companion object {
        private const val TAG = "AudioChunkQueue"
        private const val CHUNKS_DIR = "audio_chunks"
        private const val MAX_QUEUE_SIZE = 50
    }

    private val queue = ConcurrentLinkedQueue<AudioChunk>()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val _totalChunksProcessed = MutableStateFlow(0)
    val totalChunksProcessed: StateFlow<Int> = _totalChunksProcessed.asStateFlow()

    private val chunksDir: File by lazy {
        File(context.filesDir, CHUNKS_DIR).also { it.mkdirs() }
    }

    init {
        // Restore any persisted chunks from disk
        restorePersistedChunks()
    }

    /**
     * Enqueue a new audio chunk. Persists the WAV data to disk for durability.
     */
    fun enqueue(chunk: AudioChunk) {
        // Persist WAV to disk
        val wavFile = File(chunksDir, "${chunk.id}.wav")
        try {
            wavFile.writeBytes(chunk.wavData)
            Log.d(TAG, "Persisted chunk ${chunk.id} to ${wavFile.absolutePath} (${chunk.wavData.size} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist chunk ${chunk.id}", e)
        }

        queue.add(chunk)
        _totalChunksProcessed.value++

        // Enforce max queue size — discard oldest
        while (queue.size > MAX_QUEUE_SIZE) {
            val oldest = queue.poll()
            if (oldest != null) {
                deletePersistedChunk(oldest.id)
                Log.w(TAG, "Queue overflow, discarded oldest chunk: ${oldest.id}")
            }
        }

        _queueSize.value = queue.size
        Log.d(TAG, "Enqueued chunk ${chunk.id}, queue size: ${queue.size}")
    }

    /**
     * Peek at the next chunk without removing it.
     */
    fun peek(): AudioChunk? = queue.peek()

    /**
     * Dequeue the next chunk for upload processing.
     */
    fun dequeue(): AudioChunk? {
        val chunk = queue.poll()
        if (chunk != null) {
            _queueSize.value = queue.size
        }
        return chunk
    }

    /**
     * Mark a chunk as successfully uploaded and delete its persisted file.
     */
    fun markUploaded(chunkId: String) {
        deletePersistedChunk(chunkId)
        Log.d(TAG, "Chunk $chunkId marked as uploaded and deleted from disk")
    }

    /**
     * Get a snapshot of all pending chunks (for monitoring/debugging).
     */
    fun getPendingChunks(): List<AudioChunk> = queue.toList()

    /**
     * Clear all pending chunks and their persisted files.
     */
    fun clear() {
        queue.forEach { deletePersistedChunk(it.id) }
        queue.clear()
        _queueSize.value = 0
        Log.d(TAG, "Queue cleared")
    }

    private fun deletePersistedChunk(chunkId: String) {
        val wavFile = File(chunksDir, "${chunkId}.wav")
        if (wavFile.exists()) {
            wavFile.delete()
        }
    }

    private fun restorePersistedChunks() {
        val wavFiles = chunksDir.listFiles { _, name -> name.endsWith(".wav") } ?: return
        val restoredCount = wavFiles.size

        wavFiles.sortBy { it.lastModified() }
        wavFiles.forEach { file ->
            try {
                val wavData = file.readBytes()
                val chunk = AudioChunk(
                    id = file.nameWithoutExtension,
                    wavData = wavData,
                    timestamp = java.time.LocalDateTime.now(), // approximate
                    durationSeconds = WavEncoder.durationSeconds(
                        (wavData.size - 44) / 2 // subtract header, 2 bytes per sample
                    ),
                    sampleCount = (wavData.size - 44) / 2
                )
                queue.add(chunk)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore chunk from ${file.name}", e)
            }
        }

        _queueSize.value = queue.size
        if (restoredCount > 0) {
            Log.d(TAG, "Restored $restoredCount chunks from disk")
        }
    }
}
