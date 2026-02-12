package com.satwik.aimemory.ui.screens.listening

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.satwik.aimemory.data.model.MemorySummary
import com.satwik.aimemory.data.model.PipelineState
import com.satwik.aimemory.network.ApiGateway
import com.satwik.aimemory.network.ChunkUploadWorker
import com.satwik.aimemory.network.SummaryPoller
import com.satwik.aimemory.service.AudioCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListeningViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Real pipeline state from the AudioCaptureService.
     */
    val pipelineState: StateFlow<PipelineState> = AudioCaptureService.pipelineState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PipelineState.Idle)

    /**
     * Whether the capture service is currently running.
     */
    val isServiceRunning: StateFlow<Boolean> = AudioCaptureService.isRunning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Whether the OpenClaw backend is reachable (health check passed).
     * When false, audio chunks queue locally but uploads are blocked.
     */
    val isBackendReachable: StateFlow<Boolean> = ApiGateway.isBackendReachable

    /**
     * Number of audio chunks pending upload.
     */
    val pendingChunkCount: StateFlow<Int> =
        AudioCaptureService.chunkQueue?.queueSize
            ?: MutableStateFlow(0)

    /**
     * Number of chunks successfully uploaded to OpenClaw.
     */
    val uploadedChunkCount: StateFlow<Int> = ChunkUploadWorker.uploadedCount

    /**
     * Whether an upload is currently in progress.
     */
    val isUploading: StateFlow<Boolean> = ChunkUploadWorker.isUploading

    /**
     * Audio waveform amplitudes for visualization (0.0–1.0, last ~40 values).
     */
    val waveformAmplitudes: StateFlow<List<Float>> = AudioCaptureService.waveformAmplitudes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Most recent memory summary from the backend.
     * Falls back to null when no summary is available (HTTP 204).
     */
    val recentSummary: StateFlow<MemorySummary?> = SummaryPoller.latestSummary
        .map { response ->
            response?.let {
                MemorySummary(
                    id = it.id ?: "live-${System.currentTimeMillis()}",
                    timestamp = java.time.LocalDateTime.now(),
                    topics = it.topics ?: emptyList(),
                    decisions = it.decisions ?: emptyList(),
                    tasks = it.tasks ?: emptyList(),
                    emotionalTone = it.emotionalTone ?: "neutral",
                    isPinned = false,
                    rawSnippet = it.rawSnippet ?: ""
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Start summary polling when the ViewModel is created
        SummaryPoller.start()
    }

    override fun onCleared() {
        super.onCleared()
        SummaryPoller.stop()
    }

    /**
     * Start or stop the audio capture service.
     */
    fun toggleListening() {
        val context = getApplication<Application>()
        if (AudioCaptureService.isRunning.value) {
            AudioCaptureService.stop(context)
        } else {
            AudioCaptureService.start(context)
        }
    }

    /**
     * Force an immediate summary poll.
     */
    fun refreshSummary() {
        viewModelScope.launch {
            SummaryPoller.pollNow()
        }
    }
}
