package com.sleep8.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmCancelReason
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmListUiState())
    val uiState: StateFlow<AlarmListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val records = alarmRepository.getAllRecordsNewestFirst()
            val items = records
                .filter { record ->
                    record.status == AlarmStatus.SCHEDULED ||
                        (record.status == AlarmStatus.CANCELED && record.canceledReason == AlarmCancelReason.USER_TOGGLE_OFF)
                }
                .take(1)
                .map { record ->
                    val isPast = record.triggerAt < now
                    AlarmListItem(
                        id = record.id,
                        timeText = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(record.triggerAt)),
                        subtitle = buildSubtitle(record, isPast),
                        enabled = record.status == AlarmStatus.SCHEDULED,
                        toggleEnabled = !isPast
                    )
                }
            _uiState.value = _uiState.value.copy(items = items)
        }
    }

    fun onToggle(id: Long, enabled: Boolean) {
        if (_uiState.value.updatingIds.contains(id)) return
        _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds + id)
        viewModelScope.launch {
            val record = alarmRepository.getRecord(id)
            if (record != null) {
                if (enabled && record.status != AlarmStatus.SCHEDULED) {
                    enableRecord(record)
                } else if (!enabled && record.status == AlarmStatus.SCHEDULED) {
                    alarmScheduler.cancelAlarm(record, AlarmCancelReason.USER_TOGGLE_OFF)
                }
            }
            refresh()
            _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds - id)
        }
    }

    private suspend fun enableRecord(record: AlarmRecord) {
        if (record.triggerAt < System.currentTimeMillis()) {
            return
        }
        alarmScheduler.enableExisting(record)
    }

    private fun buildSubtitle(record: AlarmRecord, isPast: Boolean): String {
        if (isPast) return "Past alarm"
        return "Scheduled from screen-off"
    }
}
