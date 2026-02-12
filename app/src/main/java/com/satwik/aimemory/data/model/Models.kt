package com.satwik.aimemory.data.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Represents the current state of the audio processing pipeline.
 * The UI reacts to these states to show appropriate animations and text.
 */
sealed class PipelineState {
    data object Idle : PipelineState()
    data object Listening : PipelineState()
    data class Processing(val segmentId: String? = null) : PipelineState()
    data object Summarizing : PipelineState()
    data object Uploading : PipelineState()
    data class Error(val message: String, val isRetryable: Boolean = true) : PipelineState()
}

/**
 * A compressed semantic summary of one hour of conversation.
 */
data class MemorySummary(
    val id: String,
    val timestamp: LocalDateTime,
    val topics: List<String>,
    val decisions: List<String>,
    val tasks: List<String>,
    val emotionalTone: String,
    val isPinned: Boolean = false,
    val rawSnippet: String = ""
)

/**
 * A collection of hourly summaries grouped by day.
 */
data class DailySummary(
    val date: LocalDate,
    val hourlySummaries: List<MemorySummary>
)

/**
 * A reference to a source memory used to generate an answer.
 */
data class SourceReference(
    val summaryId: String,
    val date: LocalDate,
    val hourLabel: String,
    val relevanceScore: Float = 0f
)

/**
 * The result of a semantic retrieval query.
 */
data class QueryResult(
    val query: String,
    val answer: String,
    val sourceReferences: List<SourceReference>,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * UI state for the Ask & Retrieve screen's query lifecycle.
 */
sealed class QueryState {
    data object Idle : QueryState()
    data object Searching : QueryState()
    data class Success(val result: QueryResult) : QueryState()
    data class Error(val message: String) : QueryState()
}

/**
 * Connection and auth status indicators for Settings screen.
 */
data class SystemStatus(
    val micPermissionGranted: Boolean = false,
    val openClawConnected: Boolean = false,
    val googleDriveLinked: Boolean = false,
    val isListeningServiceRunning: Boolean = false
)
