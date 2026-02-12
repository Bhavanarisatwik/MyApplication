package com.satwik.aimemory.ui.screens.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satwik.aimemory.data.mock.MockData
import com.satwik.aimemory.data.model.QueryResult
import com.satwik.aimemory.data.model.QueryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

            // Simulate semantic search delay (will be real API call in Phase 5)
            delay(2000)

            val result = MockData.mockQueryResult.copy(query = query)
            _queryState.value = QueryState.Success(result)
            _queryHistory.value = listOf(result) + _queryHistory.value
            _queryText.value = ""
        }
    }

    fun clearQuery() {
        _queryState.value = QueryState.Idle
        _queryText.value = ""
    }
}
