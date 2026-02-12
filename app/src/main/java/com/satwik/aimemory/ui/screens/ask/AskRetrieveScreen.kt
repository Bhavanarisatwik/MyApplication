package com.satwik.aimemory.ui.screens.ask

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satwik.aimemory.data.model.QueryResult
import com.satwik.aimemory.data.model.QueryState
import com.satwik.aimemory.data.model.SourceReference
import com.satwik.aimemory.ui.theme.*
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AskRetrieveScreen(
    viewModel: AskViewModel = viewModel(),
    onNavigateToTimeline: (String) -> Unit = {}
) {
    val queryState by viewModel.queryState.collectAsStateWithLifecycle()
    val queryHistory by viewModel.queryHistory.collectAsStateWithLifecycle()
    val queryText by viewModel.queryText.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Text(
            text = "Ask Your Memory",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
        )

        Text(
            text = "Query your past conversations naturally",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
        )

        // Query Input
        QueryInputField(
            queryText = queryText,
            onQueryChange = { viewModel.updateQueryText(it) },
            onSubmit = { viewModel.submitQuery() },
            isSearching = queryState is QueryState.Searching
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (queryState) {
                is QueryState.Idle -> {
                    if (queryHistory.isEmpty()) {
                        EmptyStateHint()
                    } else {
                        QueryHistoryList(
                            history = queryHistory,
                            onNavigateToTimeline = onNavigateToTimeline
                        )
                    }
                }
                is QueryState.Searching -> {
                    SemanticSearchAnimation()
                }
                is QueryState.Success -> {
                    val result = (queryState as QueryState.Success).result
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AnswerCard(
                                result = result,
                                onSourceClick = { onNavigateToTimeline(it.summaryId) }
                            )
                        }
                        if (queryHistory.size > 1) {
                            item {
                                Text(
                                    text = "Previous Queries",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(queryHistory.drop(1)) { past ->
                                PastQueryCard(
                                    result = past,
                                    onSourceClick = { onNavigateToTimeline(it.summaryId) }
                                )
                            }
                        }
                    }
                }
                is QueryState.Error -> {
                    ErrorCard(message = (queryState as QueryState.Error).message)
                }
            }
        }
    }
}

@Composable
private fun QueryInputField(
    queryText: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSearching: Boolean
) {
    OutlinedTextField(
        value = queryText,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "What decisions did we make about...",
                color = TextMuted
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentTeal,
            unfocusedBorderColor = DarkSurfaceVariant,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            cursorColor = AccentTeal,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        trailingIcon = {
            IconButton(
                onClick = onSubmit,
                enabled = queryText.isNotBlank() && !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AccentTeal,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send query",
                        tint = if (queryText.isNotBlank()) AccentTeal else TextMuted
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSubmit() }),
        singleLine = true
    )
}

@Composable
private fun SemanticSearchAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "semanticSearch")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "searchProgress"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier.size(160.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val nodeCount = 8
                val radius = size.minDimension / 3f

                // Draw connecting lines
                for (i in 0 until nodeCount) {
                    val angle1 = (progress * 360f + i * (360f / nodeCount)) * (Math.PI / 180f)
                    val x1 = center.x + (radius * cos(angle1)).toFloat()
                    val y1 = center.y + (radius * sin(angle1)).toFloat()

                    val nextIndex = (i + 1) % nodeCount
                    val angle2 = (progress * 360f + nextIndex * (360f / nodeCount)) * (Math.PI / 180f)
                    val x2 = center.x + (radius * cos(angle2)).toFloat()
                    val y2 = center.y + (radius * sin(angle2)).toFloat()

                    drawLine(
                        color = AccentTeal.copy(alpha = pulseAlpha * 0.4f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )

                    // Cross connections for visual complexity
                    val crossIndex = (i + 3) % nodeCount
                    val angle3 = (progress * 360f + crossIndex * (360f / nodeCount)) * (Math.PI / 180f)
                    val x3 = center.x + (radius * cos(angle3)).toFloat()
                    val y3 = center.y + (radius * sin(angle3)).toFloat()

                    drawLine(
                        color = AccentCyan.copy(alpha = pulseAlpha * 0.2f),
                        start = Offset(x1, y1),
                        end = Offset(x3, y3),
                        strokeWidth = 1f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw nodes
                for (i in 0 until nodeCount) {
                    val angle = (progress * 360f + i * (360f / nodeCount)) * (Math.PI / 180f)
                    val x = center.x + (radius * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()

                    drawCircle(
                        color = AccentTeal.copy(alpha = pulseAlpha),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // Center node
                drawCircle(
                    color = WarmAmber.copy(alpha = pulseAlpha),
                    radius = 6.dp.toPx(),
                    center = center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Searching across your memories...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AnswerCard(
    result: QueryResult,
    onSourceClick: (SourceReference) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Query echo
            Text(
                text = result.query,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(12.dp))

            // Answer
            Text(
                text = result.answer,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Source references
            Text(
                text = "Sources",
                style = MaterialTheme.typography.labelLarge,
                color = AccentTeal
            )

            Spacer(modifier = Modifier.height(8.dp))

            result.sourceReferences.forEach { source ->
                SourceReferenceChip(source = source, onClick = { onSourceClick(source) })
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SourceReferenceChip(
    source: SourceReference,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${source.date.format(DateTimeFormatter.ofPattern("MMM d"))} at ${source.hourLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        // Relevance indicator
        Text(
            text = "${(source.relevanceScore * 100).toInt()}% match",
            style = MaterialTheme.typography.labelSmall,
            color = SuccessGreen
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun PastQueryCard(
    result: QueryResult,
    onSourceClick: (SourceReference) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = result.query,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = result.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun QueryHistoryList(
    history: List<QueryResult>,
    onNavigateToTimeline: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Recent Queries",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(history) { result ->
            PastQueryCard(
                result = result,
                onSourceClick = { onNavigateToTimeline(it.summaryId) }
            )
        }
    }
}

@Composable
private fun EmptyStateHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ask anything about your past conversations",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try: \"What tasks were assigned yesterday?\"",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = ErrorRed.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed
                )
            }
        }
    }
}
