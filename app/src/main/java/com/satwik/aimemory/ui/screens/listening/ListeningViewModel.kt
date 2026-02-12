package com.satwik.aimemory.ui.screens.listening

import androidx.lifecycle.ViewModel
import com.satwik.aimemory.data.mock.MockData
import com.satwik.aimemory.data.model.MemorySummary
import com.satwik.aimemory.data.model.PipelineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ListeningViewModel : ViewModel() {

    private val _pipelineState = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    private val _recentSummary = MutableStateFlow<MemorySummary?>(MockData.recentSummary)
    val recentSummary: StateFlow<MemorySummary?> = _recentSummary.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun toggleListening() {
        val current = _pipelineState.value
        _pipelineState.value = when (current) {
            is PipelineState.Idle, is PipelineState.Error -> {
                _isServiceRunning.value = true
                PipelineState.Listening
            }
            else -> {
                _isServiceRunning.value = false
                PipelineState.Idle
            }
        }
    }

    // Will be called by the actual service in Phase 2
    fun updatePipelineState(state: PipelineState) {
        _pipelineState.value = state
    }
}
