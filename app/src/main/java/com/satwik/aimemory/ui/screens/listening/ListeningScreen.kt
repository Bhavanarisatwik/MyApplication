package com.satwik.aimemory.ui.screens.listening

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satwik.aimemory.data.model.MemorySummary
import com.satwik.aimemory.data.model.PipelineState
import com.satwik.aimemory.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max

@Composable
fun ListeningScreen(
    viewModel: ListeningViewModel = viewModel()
) {
    val pipelineState by viewModel.pipelineState.collectAsStateWithLifecycle()
    val recentSummary by viewModel.recentSummary.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val pendingChunks by viewModel.pendingChunkCount.collectAsStateWithLifecycle()
    val waveformAmplitudes by viewModel.waveformAmplitudes.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Track permission state
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Permission launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted && hasNotificationPermission) {
            viewModel.toggleListening()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        // Even if notification denied, we can still start the service
        if (hasMicPermission) {
            viewModel.toggleListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        // Animated Orb
        Box(
            modifier = Modifier
                .size(240.dp)
                .weight(0.4f),
            contentAlignment = Alignment.Center
        ) {
            BreathingOrb(pipelineState = pipelineState)
        }

        // Status Text
        StatusText(
            pipelineState = pipelineState,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // Live Audio Waveform
        if (isServiceRunning && waveformAmplitudes.isNotEmpty()) {
            AudioWaveform(
                amplitudes = waveformAmplitudes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Pending chunks indicator
        if (pendingChunks > 0) {
            Text(
                text = "$pendingChunks chunk${if (pendingChunks > 1) "s" else ""} pending upload",
                style = MaterialTheme.typography.labelSmall,
                color = WarmAmber,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Toggle Button with permission handling
        FloatingActionButton(
            onClick = {
                if (isServiceRunning) {
                    // Stop service
                    viewModel.toggleListening()
                } else {
                    // Check permissions before starting
                    if (!hasMicPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.toggleListening()
                    }
                }
            },
            containerColor = if (isServiceRunning) ErrorRed.copy(alpha = 0.8f) else AccentTeal,
            contentColor = Color.White,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isServiceRunning) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isServiceRunning) "Stop listening" else "Start listening",
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Summary Card
        AnimatedVisibility(
            visible = recentSummary != null,
            enter = fadeIn(animationSpec = tween(800)) +
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(800, easing = EaseOutCubic)
                    ),
            modifier = Modifier.weight(0.3f)
        ) {
            recentSummary?.let { summary ->
                RecentSummaryCard(summary = summary)
            }
        }

        Spacer(modifier = Modifier.weight(0.05f))
    }
}

@Composable
private fun BreathingOrb(pipelineState: PipelineState) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbBreathing")

    val isActive = pipelineState is PipelineState.Listening ||
                   pipelineState is PipelineState.Processing

    // Breathing scale animation
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 1200 else 3000,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isActive) 0.6f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 800 else 2500,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Rotation for waveform effect
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 4000 else 12000,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    val orbColor by animateColorAsState(
        targetValue = when (pipelineState) {
            is PipelineState.Idle -> OrbIdle
            is PipelineState.Listening -> OrbActive
            is PipelineState.Processing -> AccentCyan
            is PipelineState.Summarizing -> WarmAmber
            is PipelineState.Uploading -> SuccessGreen
            is PipelineState.Error -> ErrorRed
        },
        animationSpec = tween(600),
        label = "orbColor"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 3f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    orbColor.copy(alpha = glowAlpha * 0.3f),
                    orbColor.copy(alpha = glowAlpha * 0.1f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 1.8f * breathScale
            ),
            center = center,
            radius = baseRadius * 1.8f * breathScale
        )

        // Middle glow ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    orbColor.copy(alpha = glowAlpha * 0.5f),
                    orbColor.copy(alpha = glowAlpha * 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 1.3f * breathScale
            ),
            center = center,
            radius = baseRadius * 1.3f * breathScale
        )

        // Core orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    orbColor.copy(alpha = 0.9f),
                    orbColor.copy(alpha = 0.6f),
                    orbColor.copy(alpha = 0.3f)
                ),
                center = center,
                radius = baseRadius * breathScale
            ),
            center = center,
            radius = baseRadius * breathScale
        )

        // Waveform particles when active
        if (isActive) {
            drawWaveParticles(center, baseRadius, rotation, orbColor, glowAlpha)
        }
    }
}

private fun DrawScope.drawWaveParticles(
    center: Offset,
    baseRadius: Float,
    rotation: Float,
    color: Color,
    alpha: Float
) {
    val particleCount = 12
    for (i in 0 until particleCount) {
        val angle = (rotation + i * (360f / particleCount)) * (Math.PI / 180f)
        val distance = baseRadius * 1.15f
        val particleX = center.x + (distance * cos(angle)).toFloat()
        val particleY = center.y + (distance * sin(angle)).toFloat()
        val particleRadius = 3.dp.toPx() * (0.5f + alpha)

        drawCircle(
            color = color.copy(alpha = alpha * 0.8f),
            radius = particleRadius,
            center = Offset(particleX, particleY)
        )
    }
}

@Composable
private fun StatusText(
    pipelineState: PipelineState,
    modifier: Modifier = Modifier
) {
    val text = when (pipelineState) {
        is PipelineState.Idle -> "Ready to listen"
        is PipelineState.Listening -> "Listening privately..."
        is PipelineState.Processing -> "Processing recent speech..."
        is PipelineState.Summarizing -> "Summarizing this hour's conversations..."
        is PipelineState.Uploading -> "Storing memory securely..."
        is PipelineState.Error -> pipelineState.message
    }

    val textColor = when (pipelineState) {
        is PipelineState.Error -> ErrorRed
        is PipelineState.Idle -> TextMuted
        else -> TextSecondary
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun RecentSummaryCard(summary: MemorySummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Latest Memory",
                style = MaterialTheme.typography.labelLarge,
                color = AccentTeal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = summary.topics.joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = summary.rawSnippet.ifEmpty {
                    summary.decisions.joinToString(". ")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = summary.emotionalTone,
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmAmber
                )
                Text(
                    text = "${summary.timestamp.hour}:${summary.timestamp.minute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

// ── Live Audio Waveform ───────────────────────────
@Composable
private fun AudioWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val barCount = amplitudes.size
    if (barCount == 0) return

    Canvas(modifier = modifier) {
        val barWidth = 6.dp.toPx()
        val gap = 2.dp.toPx()
        val totalBarWidth = barWidth + gap
        val centerY = size.height / 2f

        // Draw from right to left (newest on right)
        val maxBars = (size.width / totalBarWidth).toInt()
        val startIndex = max(0, barCount - maxBars)

        for (i in startIndex until barCount) {
            val x = (i - startIndex) * totalBarWidth
            val amplitude = amplitudes[i].coerceIn(0f, 1f)

            // Scale amplitude for visual impact (sqrt makes small values more visible)
            val scaledAmp = kotlin.math.sqrt(amplitude) * 0.9f + 0.05f
            val barHeight = scaledAmp * size.height

            val alpha = (0.3f + amplitude * 0.7f).coerceIn(0.3f, 1f)

            // Draw bar centered vertically
            drawRoundRect(
                color = AccentTeal.copy(alpha = alpha),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
