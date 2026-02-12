package com.sleep8.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmHistoryViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmHistoryUiState())
    val uiState: StateFlow<AlarmHistoryUiState> = _uiState.asStateFlow()
    private var nextOffset = 0

    init {
        refresh()
    }

    fun refresh() {
        val selectedId = _uiState.value.selectedAlarm?.id
        nextOffset = 0
        _uiState.update { it.copy(isLoadingMore = false) }
        viewModelScope.launch {
            val (firstPage, hasMore) = loadPage(offset = 0)
            nextOffset = firstPage.size
            val selected = selectedId?.let { id ->
                firstPage.firstOrNull { alarm -> alarm.id == id } ?: alarmRepository.getRecord(id)
            }
            _uiState.update {
                it.copy(
                    alarms = firstPage,
                    hasMore = hasMore,
                    isLoadingMore = false,
                    selectedAlarm = selected
                )
            }
        }
    }

    fun loadAlarm(alarmId: Long?) {
        if (alarmId == null || alarmId <= 0) {
            _uiState.value = _uiState.value.copy(selectedAlarm = null)
            return
        }
        viewModelScope.launch {
            val record = alarmRepository.getRecord(alarmId)
            _uiState.value = _uiState.value.copy(selectedAlarm = record)
        }
    }

    fun loadNextPage() {
        val previous = _uiState.getAndUpdate { current ->
            if (current.isLoadingMore || !current.hasMore) {
                current
            } else {
                current.copy(isLoadingMore = true)
            }
        }
        if (previous.isLoadingMore || !previous.hasMore) return
        viewModelScope.launch {
            val (nextPage, hasMore) = loadPage(offset = nextOffset)
            nextOffset += nextPage.size
            _uiState.update {
                it.copy(
                    alarms = it.alarms + nextPage,
                    hasMore = hasMore,
                    isLoadingMore = false
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            alarmRepository.clearAllRecords()
            nextOffset = 0
            _uiState.value = AlarmHistoryUiState()
        }
    }

    private suspend fun loadPage(offset: Int): Pair<List<AlarmRecord>, Boolean> {
        val records = alarmRepository.getRecordsNewestFirstPaged(
            limit = PAGE_SIZE + 1,
            offset = offset
        )
        val hasMore = records.size > PAGE_SIZE
        val page = if (hasMore) records.take(PAGE_SIZE) else records
        return page to hasMore
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
