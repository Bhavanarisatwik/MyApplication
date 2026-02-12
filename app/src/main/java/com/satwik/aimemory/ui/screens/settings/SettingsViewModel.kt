package com.satwik.aimemory.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.satwik.aimemory.data.model.SystemStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {

    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    private val _retentionDaysHourly = MutableStateFlow(7)
    val retentionDaysHourly: StateFlow<Int> = _retentionDaysHourly.asStateFlow()

    private val _retentionDaysDaily = MutableStateFlow(90)
    val retentionDaysDaily: StateFlow<Int> = _retentionDaysDaily.asStateFlow()

    private val _offlineEncryptionEnabled = MutableStateFlow(false)
    val offlineEncryptionEnabled: StateFlow<Boolean> = _offlineEncryptionEnabled.asStateFlow()

    fun updateMicPermission(granted: Boolean) {
        _systemStatus.value = _systemStatus.value.copy(micPermissionGranted = granted)
    }

    fun toggleOfflineEncryption() {
        _offlineEncryptionEnabled.value = !_offlineEncryptionEnabled.value
    }

    fun clearLocalCache() {
        // Will be implemented in Phase 4
    }

    fun linkGoogleDrive() {
        // Will trigger OAuth flow in Phase 4
    }
}
