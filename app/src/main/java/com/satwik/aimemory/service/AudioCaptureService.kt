package com.satwik.aimemory.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.konovalov.vad.webrtc.Vad
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import com.satwik.aimemory.MainActivity
import com.satwik.aimemory.R
import com.satwik.aimemory.data.model.PipelineState
import com.satwik.aimemory.network.ChunkUploadWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service for continuous audio capture with WebRTC VAD.
 *
 * Pipeline: AudioRecord → VAD → ChunkManager → WavEncoder → ChunkQueue
 *
 * Uses a coroutine-based audio loop running on a dedicated IO dispatcher.
 * VAD gates recording: PCM is only accumulated when speech is detected.
 * Chunks are finalized after 600ms sustained silence or 30s max duration.
 */
class AudioCaptureService : Service() {

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "audio_capture_channel"

        // Audio config
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SIZE = 320 // 20ms at 16kHz
        private const val SILENCE_TIMEOUT_MS = 600L
        private const val MAX_CHUNK_SECONDS = 30f

        // Shared state for UI observation
        private val _pipelineState = MutableStateFlow<PipelineState>(PipelineState.Idle)
        val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        /** Raw audio RMS level (0.0–1.0) for waveform visualization. */
        private val _audioRmsLevel = MutableStateFlow(0f)
        val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

        /** Recent RMS history for waveform bars (last ~40 values). */
        private val _waveformAmplitudes = MutableStateFlow<List<Float>>(emptyList())
        val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes.asStateFlow()

        private var _chunkQueue: AudioChunkQueue? = null
        val chunkQueue: AudioChunkQueue? get() = _chunkQueue

        fun start(context: Context) {
            val intent = Intent(context, AudioCaptureService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioCaptureService::class.java)
            context.stopService(intent)
        }
    }

    private var audioRecord: AudioRecord? = null
    private var vad: VadWebRTC? = null
    private var chunkManager: AudioChunkManager? = null
    private var serviceScope: CoroutineScope? = null
    private var captureJob: Job? = null

    // Silence tracking
    private var lastSpeechTimestamp = 0L
    private var isSpeechActive = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        _chunkQueue = AudioChunkQueue(applicationContext)
        chunkManager = AudioChunkManager(
            maxChunkDurationSeconds = MAX_CHUNK_SECONDS,
            sampleRate = SAMPLE_RATE
        )
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Listening privately..."),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        _isRunning.value = true
        _pipelineState.value = PipelineState.Listening

        startAudioCapture()

        // Start upload worker to drain queued chunks to OpenClaw
        _chunkQueue?.let { ChunkUploadWorker.start(it) }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")

        stopAudioCapture()
        ChunkUploadWorker.stop()
        vad?.close()
        vad = null

        _isRunning.value = false
        _pipelineState.value = PipelineState.Idle

        serviceScope?.cancel()
        serviceScope = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ──────────────────────────────────────────────
    // Audio capture loop
    // ──────────────────────────────────────────────

    private fun startAudioCapture() {
        // Initialize VAD
        try {
            vad = Vad.builder()
                .setSampleRate(SampleRate.SAMPLE_RATE_16K)
                .setFrameSize(FrameSize.FRAME_SIZE_320)
                .setMode(Mode.VERY_AGGRESSIVE)
                .build()
            Log.d(TAG, "WebRTC VAD initialized (VERY_AGGRESSIVE, 16kHz, 320 frame)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VAD", e)
            _pipelineState.value = PipelineState.Error("VAD initialization failed", isRetryable = true)
            stopSelf()
            return
        }

        // Initialize AudioRecord
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(FRAME_SIZE * 2) // Ensure buffer is at least one frame

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord failed to initialize")
            }

            audioRecord?.startRecording()

            // ═══ DIAGNOSTIC: Log actual AudioRecord config ═══
            val actualSampleRate = audioRecord?.sampleRate ?: -1
            val actualChannelConfig = audioRecord?.channelConfiguration
            val actualEncoding = audioRecord?.audioFormat
            Log.w(TAG, "╔══════════════ AUDIO DIAGNOSTICS ══════════════")
            Log.w(TAG, "║ Requested sample rate: $SAMPLE_RATE Hz")
            Log.w(TAG, "║ Actual sample rate:    $actualSampleRate Hz")
            Log.w(TAG, "║ Channel config:        $actualChannelConfig (expected ${AudioFormat.CHANNEL_IN_MONO})")
            Log.w(TAG, "║ Audio encoding:        $actualEncoding (expected ${AudioFormat.ENCODING_PCM_16BIT})")
            Log.w(TAG, "║ Frame size:            $FRAME_SIZE samples")
            Log.w(TAG, "║ Buffer size:           $bufferSize bytes")
            Log.w(TAG, "║ VAD expects:           16kHz, 320 samples, VERY_AGGRESSIVE")
            Log.w(TAG, "╚══════════════════════════════════════════════")

            if (actualSampleRate != SAMPLE_RATE) {
                Log.e(TAG, "⚠ SAMPLE RATE MISMATCH! Requested $SAMPLE_RATE but got $actualSampleRate")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission not granted", e)
            _pipelineState.value = PipelineState.Error("Microphone permission required", isRetryable = false)
            stopSelf()
            return
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            _pipelineState.value = PipelineState.Error("Audio capture failed: ${e.message}", isRetryable = true)
            stopSelf()
            return
        }

        // Start capture loop on IO dispatcher
        var frameCount = 0L
        captureJob = serviceScope?.launch {
            val frameBuffer = ShortArray(FRAME_SIZE)
            Log.d(TAG, "Audio capture loop started")

            try {
                while (isActive) {
                    val samplesRead = audioRecord?.read(frameBuffer, 0, FRAME_SIZE) ?: -1

                    if (samplesRead <= 0) {
                        Log.w(TAG, "AudioRecord.read returned $samplesRead")
                        delay(10)
                        continue
                    }

                    frameCount++

                    // ═══ DIAGNOSTIC: Compute and log RMS ═══
                    val frame = frameBuffer.copyOf(samplesRead)
                    val rms = computeRms(frame)
                    val normalizedRms = (rms / 32768f).coerceIn(0f, 1f)
                    _audioRmsLevel.value = normalizedRms

                    // Update waveform history (keep last 40 values)
                    val currentAmps = _waveformAmplitudes.value.toMutableList()
                    currentAmps.add(normalizedRms)
                    if (currentAmps.size > 40) currentAmps.removeAt(0)
                    _waveformAmplitudes.value = currentAmps

                    // Log every 50th frame (~1 per second) to avoid flood
                    if (frameCount % 50 == 0L) {
                        Log.d(TAG, "[DIAG] frame=$frameCount samples=$samplesRead RMS=${"%.1f".format(rms)} normalized=${"%.4f".format(normalizedRms)}")
                    }

                    processFrame(frame)
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Capture loop cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Capture loop error", e)
                _pipelineState.value = PipelineState.Error("Capture error: ${e.message}", isRetryable = true)
            }

            Log.d(TAG, "Audio capture loop ended (total frames: $frameCount)")
        }
    }

    private fun stopAudioCapture() {
        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null

        // Finalize any in-progress chunk
        chunkManager?.finalizeChunk()?.let { chunk ->
            _chunkQueue?.enqueue(chunk)
            Log.d(TAG, "Finalized remaining chunk on stop: ${chunk.id}")
        }
    }

    private fun processFrame(frame: ShortArray) {
        val localVad = vad ?: run {
            Log.e(TAG, "[PIPE] vad is NULL — cannot process frame")
            return
        }
        val localChunkManager = chunkManager ?: run {
            Log.e(TAG, "[PIPE] chunkManager is NULL — cannot process frame")
            return
        }
        val localQueue = _chunkQueue ?: run {
            Log.e(TAG, "[PIPE] chunkQueue is NULL — cannot process frame")
            return
        }

        val isSpeech = localVad.isSpeech(frame)
        val now = System.currentTimeMillis()

        if (isSpeech) {
            // Speech detected
            if (!isSpeechActive) {
                // Speech onset — start a new chunk
                isSpeechActive = true
                _pipelineState.value = PipelineState.Processing("speech")
                localChunkManager.startChunk()
                updateNotification("Listening — speech detected")
                Log.w(TAG, "[PIPE] ▶ SPEECH ONSET — startChunk() called, isCapturing=${localChunkManager.isCurrentlyCapturing()}")
            }

            lastSpeechTimestamp = now

            // Append samples; may return auto-split chunk
            val autoSplitChunk = localChunkManager.appendSamples(frame)
            if (autoSplitChunk != null) {
                localQueue.enqueue(autoSplitChunk)
                Log.w(TAG, "[PIPE] ⏫ AUTO-SPLIT chunk enqueued: ${autoSplitChunk.id} (${autoSplitChunk.durationSeconds}s, ${autoSplitChunk.wavData.size} bytes) queueSize=${localQueue.queueSize.value}")
                // Start a fresh chunk immediately (speech is still active)
                localChunkManager.startChunk()
                localChunkManager.appendSamples(frame)
            }

            // Force flush safety: if chunk has been actively recording > 30s
            val chunkDuration = localChunkManager.currentDurationSeconds()
            if (chunkDuration >= MAX_CHUNK_SECONDS) {
                val forcedChunk = localChunkManager.finalizeChunk()
                if (forcedChunk != null) {
                    localQueue.enqueue(forcedChunk)
                    Log.w(TAG, "[PIPE] ⏫ FORCE FLUSH (${chunkDuration}s) chunk: ${forcedChunk.id} queueSize=${localQueue.queueSize.value}")
                    // Restart for continuous speech
                    localChunkManager.startChunk()
                }
            }
        } else {
            // Silence detected
            if (isSpeechActive) {
                // Still accumulating — keep appending during short silences
                localChunkManager.appendSamples(frame)

                // Check if silence has been sustained long enough
                val silenceDuration = now - lastSpeechTimestamp

                // Log silence tracking periodically (every ~200ms)
                if (silenceDuration % 200 < 25) {
                    Log.d(TAG, "[PIPE] silence=${silenceDuration}ms / ${SILENCE_TIMEOUT_MS}ms threshold, bufferSamples=${localChunkManager.currentDurationSeconds()}s")
                }

                if (silenceDuration >= SILENCE_TIMEOUT_MS) {
                    // Sustained silence — finalize chunk
                    val chunk = localChunkManager.finalizeChunk()
                    if (chunk != null) {
                        localQueue.enqueue(chunk)
                        Log.w(TAG, "[PIPE] ⏫ SILENCE FINALIZED chunk: ${chunk.id} (${chunk.durationSeconds}s, ${chunk.wavData.size} bytes) queueSize=${localQueue.queueSize.value}")
                    } else {
                        Log.w(TAG, "[PIPE] ⚠ finalizeChunk returned NULL despite isSpeechActive=true")
                    }
                    isSpeechActive = false
                    _pipelineState.value = PipelineState.Listening
                    updateNotification("Listening privately...")
                }
            }
            // else: silence while not capturing — do nothing (idle listening)
        }
    }

    /** Compute RMS (root mean square) of a PCM 16-bit frame. */
    private fun computeRms(frame: ShortArray): Float {
        var sumOfSquares = 0.0
        for (sample in frame) {
            sumOfSquares += sample.toDouble() * sample.toDouble()
        }
        return kotlin.math.sqrt(sumOfSquares / frame.size).toFloat()
    }

    // ──────────────────────────────────────────────
    // Notification management
    // ──────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when AiMemory is listening for speech"
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioCaptureService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AiMemory")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
