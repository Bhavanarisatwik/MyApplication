package com.satwik.aimemory.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satwik.aimemory.data.model.PipelineState
import com.satwik.aimemory.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    // Dashboard states
    val micGranted by viewModel.micPermissionGranted.collectAsStateWithLifecycle()
    val serviceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val pipeline by viewModel.pipelineState.collectAsStateWithLifecycle()
    val pendingChunks by viewModel.pendingChunkCount.collectAsStateWithLifecycle()
    val uploadedChunks by viewModel.uploadedChunkCount.collectAsStateWithLifecycle()
    val backendConnected by viewModel.isBackendConnected.collectAsStateWithLifecycle()
    val lastUploadTs by viewModel.lastUploadTimestamp.collectAsStateWithLifecycle()
    val uploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val lastSummaryTs by viewModel.lastSummaryPollTimestamp.collectAsStateWithLifecycle()
    val hasSummary by viewModel.hasSummary.collectAsStateWithLifecycle()
    val googleSignedIn by viewModel.isGoogleSignedIn.collectAsStateWithLifecycle()
    val driveEmail by viewModel.driveEmail.collectAsStateWithLifecycle()

    // Settings
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

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleSignInResult(result)
        }
    }

    // Derived display values
    val vadStatus = when (pipeline) {
        is PipelineState.Idle -> "Idle"
        is PipelineState.Listening -> "Silence"
        is PipelineState.Processing -> "Speech detected"
        is PipelineState.Summarizing -> "Summarizing"
        is PipelineState.Uploading -> "Uploading"
        is PipelineState.Error -> "Error"
    }
    val vadActive = pipeline is PipelineState.Processing || pipeline is PipelineState.Summarizing

    val lastUploadDisplay = if (lastUploadTs > 0L) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUploadTs))
    } else "Never"

    val lastSummaryDisplay = if (lastSummaryTs > 0L) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSummaryTs))
    } else "Never"

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

        // ── Operational Dashboard ─────────────────
        SectionHeader("Operational Dashboard")

        DashboardRow(
            icon = Icons.Default.Mic,
            label = "MIC",
            value = if (serviceRunning) "Recording" else "Stopped",
            isActive = serviceRunning,
            onClick = {
                if (!micGranted) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )

        DashboardRow(
            icon = Icons.Default.GraphicEq,
            label = "VAD",
            value = vadStatus,
            isActive = vadActive
        )

        DashboardRow(
            icon = Icons.Default.Queue,
            label = "Pending chunks",
            value = "$pendingChunks",
            isActive = pendingChunks == 0,
            activeColor = if (pendingChunks > 0) WarmAmber else SuccessGreen
        )

        DashboardRow(
            icon = Icons.Default.CloudUpload,
            label = "Uploaded chunks",
            value = if (uploading) "$uploadedChunks  ⬆" else "$uploadedChunks",
            isActive = uploadedChunks > 0
        )

        DashboardRow(
            icon = Icons.Default.Cloud,
            label = "Backend",
            value = if (backendConnected) "Connected" else "Disconnected",
            isActive = backendConnected,
            onClick = { viewModel.refreshConnectionStatus() }
        )

        DashboardRow(
            icon = Icons.Default.Schedule,
            label = "Last upload",
            value = lastUploadDisplay,
            isActive = lastUploadTs > 0L
        )

        DashboardRow(
            icon = Icons.Default.Summarize,
            label = "Summary available",
            value = if (hasSummary) "Yes ($lastSummaryDisplay)" else "No",
            isActive = hasSummary
        )

        DashboardRow(
            icon = Icons.Default.AccountCircle,
            label = "Google Auth",
            value = if (googleSignedIn) driveEmail ?: "Signed In" else "Not Signed In",
            isActive = googleSignedIn,
            onClick = {
                if (googleSignedIn) {
                    viewModel.unlinkGoogleDrive()
                } else {
                    signInLauncher.launch(viewModel.linkGoogleDrive())
                }
            }
        )

        DashboardRow(
            icon = Icons.Default.CloudDone,
            label = "Drive Upload",
            value = if (googleSignedIn) "Idle" else "Not linked",
            isActive = googleSignedIn
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Retention Policy ──────────────────────
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

        // ── Privacy Controls ──────────────────────
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

// ── Dashboard Row ─────────────────────────────────
@Composable
private fun DashboardRow(
    icon: ImageVector,
    label: String,
    value: String,
    isActive: Boolean,
    activeColor: Color = SuccessGreen,
    onClick: (() -> Unit)? = null
) {
    val statusColor = if (isActive) activeColor else TextMuted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        onClick = { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )
        }
    }
}

// ── Reusable Components ───────────────────────────

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
    iconTint: Color
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
