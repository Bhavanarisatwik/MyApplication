package com.satwik.aimemory.ui.screens.settings

import android.app.Application
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.satwik.aimemory.data.model.PipelineState
import com.satwik.aimemory.drive.GoogleSignInManager
import com.satwik.aimemory.network.ChunkUploadWorker
import com.satwik.aimemory.network.HealthCheckManager
import com.satwik.aimemory.network.SummaryPoller
import com.satwik.aimemory.service.AudioCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // ── Permission state ──────────────────────────
    private val _micPermissionGranted = MutableStateFlow(false)
    val micPermissionGranted: StateFlow<Boolean> = _micPermissionGranted.asStateFlow()

    // ── Dashboard: MIC ────────────────────────────
    val isServiceRunning: StateFlow<Boolean> = AudioCaptureService.isRunning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── Dashboard: VAD ────────────────────────────
    val pipelineState: StateFlow<PipelineState> = AudioCaptureService.pipelineState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PipelineState.Idle)

    // ── Dashboard: Chunks ─────────────────────────
    val pendingChunkCount: StateFlow<Int> =
        AudioCaptureService.chunkQueue?.queueSize ?: MutableStateFlow(0)

    val uploadedChunkCount: StateFlow<Int> = ChunkUploadWorker.uploadedCount

    // ── Dashboard: Backend ────────────────────────
    val isBackendConnected: StateFlow<Boolean> = HealthCheckManager.isConnected

    // ── Dashboard: Upload ─────────────────────────
    val lastUploadTimestamp: StateFlow<Long> = ChunkUploadWorker.lastUploadTimestamp
    val isUploading: StateFlow<Boolean> = ChunkUploadWorker.isUploading

    // ── Dashboard: Summary ────────────────────────
    val lastSummaryPollTimestamp: StateFlow<Long> = SummaryPoller.lastPollTimestamp
    val hasSummary: StateFlow<Boolean> = SummaryPoller.latestSummary
        .let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { emit(it != null) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        }

    // ── Dashboard: Google Auth ────────────────────
    val isGoogleSignedIn: StateFlow<Boolean> = GoogleSignInManager.isSignedIn
    val driveEmail: StateFlow<String?> = GoogleSignInManager.accountEmail

    // ── Settings ──────────────────────────────────
    private val _retentionDaysHourly = MutableStateFlow(7)
    val retentionDaysHourly: StateFlow<Int> = _retentionDaysHourly.asStateFlow()

    private val _retentionDaysDaily = MutableStateFlow(90)
    val retentionDaysDaily: StateFlow<Int> = _retentionDaysDaily.asStateFlow()

    private val _offlineEncryptionEnabled = MutableStateFlow(false)
    val offlineEncryptionEnabled: StateFlow<Boolean> = _offlineEncryptionEnabled.asStateFlow()

    init {
        HealthCheckManager.start()
        GoogleSignInManager.checkExistingSignIn(application)
    }

    fun updateMicPermission(granted: Boolean) {
        _micPermissionGranted.value = granted
    }

    fun toggleOfflineEncryption() {
        _offlineEncryptionEnabled.value = !_offlineEncryptionEnabled.value
    }

    fun clearLocalCache() {
        AudioCaptureService.chunkQueue?.clear()
    }

    fun getSignInIntent(): Intent {
        val context = getApplication<Application>()
        return GoogleSignInManager.getClient(context).signInIntent
    }

    fun handleSignInResult(result: ActivityResult) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                GoogleSignInManager.onSignInSuccess(account)
            }
        } catch (e: ApiException) {
            android.util.Log.e("SettingsViewModel", "Sign-in failed: ${e.statusCode}", e)
        }
    }

    fun unlinkGoogleDrive() {
        val context = getApplication<Application>()
        GoogleSignInManager.signOut(context)
    }

    fun linkGoogleDrive(): Intent = getSignInIntent()

    fun refreshConnectionStatus() {
        viewModelScope.launch {
            HealthCheckManager.checkNow()
        }
    }
}
