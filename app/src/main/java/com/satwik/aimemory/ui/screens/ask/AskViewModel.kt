package com.satwik.aimemory.ui.screens.ask

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satwik.aimemory.data.model.QueryResult
import com.satwik.aimemory.data.model.QueryState
import com.satwik.aimemory.data.model.SourceReference
import com.satwik.aimemory.network.ApiGateway
import com.satwik.aimemory.network.QueryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AskViewModel : ViewModel() {

    private val _queryState = MutableStateFlow<QueryState>(QueryState.Idle)
    val queryState: StateFlow<QueryState> = _queryState.asStateFlow()

    private val _queryHistory = MutableStateFlow<List<QueryResult>>(emptyList())
    val queryHistory: StateFlow<List<QueryResult>> = _queryHistory.asStateFlow()

    private val _queryText = MutableStateFlow("")
    val queryText: StateFlow<String> = _queryText.asStateFlow()

    fun updateQueryText(text: String) {
        _queryText.value = text
    }

    fun submitQuery() {
        val query = _queryText.value.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _queryState.value = QueryState.Searching

            val response = ApiGateway.submitQuery(query)

            if (response != null) {
                val result = mapResponseToQueryResult(query, response)
                _queryState.value = QueryState.Success(result)
                _queryHistory.value = listOf(result) + _queryHistory.value
                _queryText.value = ""
            } else {
                _queryState.value = QueryState.Error("Failed to get answer. Check backend connectivity.")
            }
        }
    }

    private fun mapResponseToQueryResult(query: String, response: QueryResponse): QueryResult {
        return QueryResult(
            query = query,
            answer = response.answer,
            sourceReferences = response.sources.map { dto ->
                SourceReference(
                    summaryId = dto.summaryId,
                    date = try {
                        // Backend expected format: "2025-01-15T14:30:00"
                         java.time.LocalDate.parse(dto.timestamp.substring(0, 10))
                    } catch (e: Exception) {
                        java.time.LocalDate.now()
                    },
                    hourLabel = try {
                         val timePart = dto.timestamp.substring(11, 16) // "HH:mm"
                         timePart
                    } catch (e: Exception) {
                        "Unknown"
                    },
                    relevanceScore = dto.relevance
                )
            }
        )
    }

    fun clearQuery() {
        _queryState.value = QueryState.Idle
        _queryText.value = ""
    }
}
