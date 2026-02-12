package com.satwik.aimemory.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satwik.aimemory.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val systemStatus by viewModel.systemStatus.collectAsStateWithLifecycle()
    val retentionHourly by viewModel.retentionDaysHourly.collectAsStateWithLifecycle()
    val retentionDaily by viewModel.retentionDaysDaily.collectAsStateWithLifecycle()
    val offlineEncryption by viewModel.offlineEncryptionEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateMicPermission(granted)
    }

    // Check initial permission state
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateMicPermission(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings & Privacy",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp)
        )

        // System Status Section
        SectionHeader("System Status")

        StatusRow(
            icon = Icons.Default.Mic,
            label = "Microphone Access",
            status = if (systemStatus.micPermissionGranted) "Granted" else "Not Granted",
            isActive = systemStatus.micPermissionGranted,
            onClick = {
                if (!systemStatus.micPermissionGranted) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )

        StatusRow(
            icon = Icons.Default.Cloud,
            label = "OpenClaw Connection",
            status = if (systemStatus.openClawConnected) "Connected" else "Not Connected",
            isActive = systemStatus.openClawConnected
        )

        StatusRow(
            icon = Icons.Default.CloudDone,
            label = "Google Drive",
            status = if (systemStatus.googleDriveLinked) "Linked" else "Not Linked",
            isActive = systemStatus.googleDriveLinked,
            onClick = { viewModel.linkGoogleDrive() }
        )

        StatusRow(
            icon = Icons.Default.Hearing,
            label = "Listening Service",
            status = if (systemStatus.isListeningServiceRunning) "Running" else "Stopped",
            isActive = systemStatus.isListeningServiceRunning
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Retention Policy Section
        SectionHeader("Retention Policy")

        RetentionInfoCard(
            title = "Raw Audio",
            description = "Deleted immediately after transcription. Never stored permanently.",
            icon = Icons.Default.DeleteForever,
            iconTint = ErrorRed
        )

        RetentionInfoCard(
            title = "Hourly Summaries",
            description = "Automatically expire after $retentionHourly days unless pinned.",
            icon = Icons.Default.Schedule,
            iconTint = WarmAmber
        )

        RetentionInfoCard(
            title = "Daily Summaries",
            description = "Persist for $retentionDaily days. Pinned memories are kept indefinitely.",
            icon = Icons.Default.CalendarToday,
            iconTint = AccentTeal
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy Controls
        SectionHeader("Privacy Controls")

        SwitchRow(
            icon = Icons.Default.Lock,
            label = "Offline Encrypted Storage",
            description = "Encrypt locally cached summaries",
            checked = offlineEncryption,
            onToggle = { viewModel.toggleOfflineEncryption() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionRow(
            icon = Icons.Default.CleaningServices,
            label = "Clear Local Cache",
            description = "Remove all locally cached data",
            onClick = { viewModel.clearLocalCache() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // App Info
        Text(
            text = "AiMemory v1.0 · Your memories, your cloud.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = AccentTeal,
        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp, top = 8.dp)
    )
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    status: String,
    isActive: Boolean,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        onClick = { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) SuccessGreen else TextMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isActive) SuccessGreen else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) SuccessGreen else TextMuted
                )
            }
        }
    }
}

@Composable
private fun RetentionInfoCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) AccentTeal else TextMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentTeal,
                    checkedTrackColor = AccentTeal.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ErrorRed.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
