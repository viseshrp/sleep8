package com.sleep8.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    private var alarmId: Long = -1L

    init {
        viewModelScope.launch {
            while (true) {
                val time = LocalTime.now()
                _uiState.value = _uiState.value.copy(currentTime = TimeUtils.formatAlarmTime(time))
                delay(1000)
            }
        }

        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.value = _uiState.value.copy(showSnooze = settings.snoozeMinutes != null)
        }
    }

    fun setAlarmId(id: Long) {
        alarmId = id
    }

    fun dismiss() {
        if (alarmId <= 0) return
        viewModelScope.launch {
            alarmRepository.markDismissed(alarmId, System.currentTimeMillis())
        }
    }

    fun snooze() {
        if (alarmId <= 0) return
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val minutes = settings.snoozeMinutes ?: return@launch
            alarmScheduler.scheduleSnooze(alarmId, minutes)
        }
    }
}

