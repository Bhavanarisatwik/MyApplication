package com.satwik.aimemory.network

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /health
 */
data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String? = null,
    @SerializedName("uptime") val uptime: Long? = null
)

/**
 * Response from POST /audio/upload
 */
data class UploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("chunk_id") val chunkId: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("transcript") val transcript: String? = null
)

/**
 * Response from GET /summary/latest
 */
data class SummaryResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("topics") val topics: List<String>? = null,
    @SerializedName("decisions") val decisions: List<String>? = null,
    @SerializedName("tasks") val tasks: List<String>? = null,
    @SerializedName("emotional_tone") val emotionalTone: String? = null,
    @SerializedName("raw_snippet") val rawSnippet: String? = null
)

/**
 * Request for POST /query
 */
data class QueryRequest(
    @SerializedName("query") val query: String
)

/**
 * Response for POST /query
 */
data class QueryResponse(
    @SerializedName("answer") val answer: String,
    @SerializedName("sources") val sources: List<SourceReferenceDto>
)

data class SourceReferenceDto(
    @SerializedName("summary_id") val summaryId: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("relevance") val relevance: Float
)
