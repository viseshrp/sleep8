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
            val record = alarmRepository.getLatestRecord()
            val items = record?.let {
                val isPast = it.triggerAt < now
                val toggleEnabled = when (it.status) {
                    AlarmStatus.SCHEDULED -> !isPast
                    AlarmStatus.CANCELED -> it.canceledReason == AlarmCancelReason.USER_TOGGLE_OFF && !isPast
                    AlarmStatus.FIRED, AlarmStatus.DISMISSED -> false
                }
                listOf(
                    AlarmListItem(
                        id = it.id,
                        timeText = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it.triggerAt)),
                        subtitle = buildSubtitle(it, isPast),
                        enabled = it.status == AlarmStatus.SCHEDULED && !isPast,
                        toggleEnabled = toggleEnabled
                    )
                )
            } ?: emptyList()
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
        return when (record.status) {
            AlarmStatus.SCHEDULED -> if (isPast) "Past alarm" else "Scheduled from screen-off"
            AlarmStatus.FIRED -> "Fired"
            AlarmStatus.DISMISSED -> "Dismissed"
            AlarmStatus.CANCELED -> when (record.canceledReason) {
                AlarmCancelReason.USER_TOGGLE_OFF -> "Disabled"
                AlarmCancelReason.REPLACED_BY_NEW_ALARM -> "Replaced by newer alarm"
                AlarmCancelReason.USER_DISARM -> "Disarmed"
                AlarmCancelReason.REBOOT_CLEANUP -> "Canceled after reboot"
                null -> "Canceled"
            }
        }
    }
}
