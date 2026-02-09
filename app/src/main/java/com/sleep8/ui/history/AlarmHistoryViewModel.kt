package com.sleep8.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmHistoryViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmHistoryUiState())
    val uiState: StateFlow<AlarmHistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val alarms = alarmRepository.getAllRecordsNewestFirst()
            val selectedId = _uiState.value.selectedAlarm?.id
            val selected = selectedId?.let { id -> alarms.firstOrNull { it.id == id } }
            _uiState.value = _uiState.value.copy(
                alarms = alarms,
                selectedAlarm = selected
            )
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

    fun clearHistory() {
        viewModelScope.launch {
            alarmRepository.clearAllRecords()
            _uiState.value = AlarmHistoryUiState()
        }
    }
}
