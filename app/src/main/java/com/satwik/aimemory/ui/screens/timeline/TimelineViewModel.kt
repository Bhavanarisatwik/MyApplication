package com.satwik.aimemory.ui.screens.timeline

import androidx.lifecycle.ViewModel
import com.satwik.aimemory.data.mock.MockData
import com.satwik.aimemory.data.model.DailySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimelineViewModel : ViewModel() {

    private val _dailySummaries = MutableStateFlow(MockData.dailySummaries)
    val dailySummaries: StateFlow<List<DailySummary>> = _dailySummaries.asStateFlow()

    private val _expandedDays = MutableStateFlow<Set<String>>(
        setOf(MockData.dailySummaries.firstOrNull()?.date?.toString() ?: "")
    )
    val expandedDays: StateFlow<Set<String>> = _expandedDays.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun toggleDayExpansion(dateKey: String) {
        _expandedDays.value = _expandedDays.value.toMutableSet().apply {
            if (contains(dateKey)) remove(dateKey) else add(dateKey)
        }
    }

    fun togglePin(summaryId: String) {
        _dailySummaries.value = _dailySummaries.value.map { daily ->
            daily.copy(
                hourlySummaries = daily.hourlySummaries.map { summary ->
                    if (summary.id == summaryId) summary.copy(isPinned = !summary.isPinned)
                    else summary
                }
            )
        }
    }
}
