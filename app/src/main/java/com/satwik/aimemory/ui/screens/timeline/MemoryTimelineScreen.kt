package com.satwik.aimemory.ui.screens.timeline

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satwik.aimemory.data.model.DailySummary
import com.satwik.aimemory.data.model.MemorySummary
import com.satwik.aimemory.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun MemoryTimelineScreen(
    viewModel: TimelineViewModel = viewModel()
) {
    val dailySummaries by viewModel.dailySummaries.collectAsStateWithLifecycle()
    val expandedDays by viewModel.expandedDays.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Text(
            text = "Memory Timeline",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dailySummaries, key = { it.date.toString() }) { daily ->
                val daysAgo = ChronoUnit.DAYS.between(daily.date, LocalDate.now())
                val ageFade = (1f - (daysAgo * 0.1f).coerceAtMost(0.5f)).coerceAtLeast(0.5f)

                DayNode(
                    dailySummary = daily,
                    isExpanded = expandedDays.contains(daily.date.toString()),
                    ageFade = ageFade,
                    onToggle = { viewModel.toggleDayExpansion(daily.date.toString()) },
                    onTogglePin = { viewModel.togglePin(it) }
                )
            }
        }
    }
}

@Composable
private fun DayNode(
    dailySummary: DailySummary,
    isExpanded: Boolean,
    ageFade: Float,
    onToggle: () -> Unit,
    onTogglePin: (String) -> Unit
) {
    val isToday = dailySummary.date == LocalDate.now()
    val dateLabel = when {
        isToday -> "Today"
        dailySummary.date == LocalDate.now().minusDays(1) -> "Yesterday"
        else -> dailySummary.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }

    Column(
        modifier = Modifier.alpha(ageFade)
    ) {
        // Day header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isToday) DarkSurfaceVariant else DarkSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Timeline dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isToday) AccentTeal else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${dailySummary.hourlySummaries.size} memories",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary
                )
            }
        }

        // Expandable hourly summaries
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dailySummary.hourlySummaries.forEach { summary ->
                    HourlyMemoryCard(
                        summary = summary,
                        onTogglePin = { onTogglePin(summary.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyMemoryCard(
    summary: MemorySummary,
    onTogglePin: () -> Unit
) {
    val borderModifier = if (summary.isPinned) {
        Modifier.border(1.dp, PinnedBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.isPinned)
                DarkSurfaceVariant.copy(alpha = 0.9f)
            else
                DarkSurface.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary.timestamp.hour}:${summary.timestamp.minute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentTeal
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Emotional tone chip
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = summary.emotionalTone,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmAmber
                            )
                        },
                        modifier = Modifier.height(24.dp),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = WarmAmber.copy(alpha = 0.3f)
                        ),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = WarmAmber.copy(alpha = 0.1f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (summary.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin memory",
                            tint = if (summary.isPinned) WarmAmber else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Topics
            Text(
                text = summary.topics.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            // Decisions
            if (summary.decisions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                summary.decisions.forEach { decision ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = decision,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Tasks
            if (summary.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                summary.tasks.forEach { task ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = AccentCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
