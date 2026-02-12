package com.satwik.aimemory.service

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes raw 16-bit mono PCM audio data into WAV format.
 * WAV = 44-byte RIFF header + raw PCM payload.
 */
object WavEncoder {

    private const val SAMPLE_RATE = 16000
    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16

    /**
     * Converts a ShortArray of PCM samples to a complete WAV ByteArray.
     */
    fun encode(pcmData: ShortArray): ByteArray {
        val pcmBytes = pcmData.size * 2 // 16-bit = 2 bytes per sample
        val totalSize = 44 + pcmBytes

        val buffer = ByteBuffer.allocate(totalSize).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalSize - 8) // ChunkSize = file size - 8
            put("WAVE".toByteArray(Charsets.US_ASCII))

            // fmt sub-chunk
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)                              // SubChunk1Size (PCM = 16)
            putShort(1)                             // AudioFormat (PCM = 1)
            putShort(CHANNELS.toShort())            // NumChannels
            putInt(SAMPLE_RATE)                     // SampleRate
            putInt(SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8) // ByteRate
            putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort()) // BlockAlign
            putShort(BITS_PER_SAMPLE.toShort())     // BitsPerSample

            // data sub-chunk
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmBytes)                        // SubChunk2Size

            // PCM payload
            pcmData.forEach { putShort(it) }
        }

        return buffer.array()
    }

    /**
     * Returns the duration in seconds for a given number of PCM samples.
     */
    fun durationSeconds(sampleCount: Int): Float {
        return sampleCount.toFloat() / SAMPLE_RATE
    }
}
