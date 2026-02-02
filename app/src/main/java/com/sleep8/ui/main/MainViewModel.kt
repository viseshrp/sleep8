package com.sleep8.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.state.StateHolder
import com.sleep8.util.PermissionUtils
import com.sleep8.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val armManager: ArmManager,
    private val stateHolder: StateHolder,
    private val alarmRepository: com.sleep8.data.repository.AlarmRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                updateState()
                delay(1000)
            }
        }
    }

    private suspend fun updateState() {
        val state = stateHolder.state.value
        val armed = state != AppState.DISARMED
        val lastScreenOffTs = stateHolder.lastScreenOffTs.value
        val pendingDeadline = stateHolder.pendingConfirmDeadlineTs.value
        val now = System.currentTimeMillis()
        val pendingRemaining = if (pendingDeadline > 0) pendingDeadline - now else 0L
        val latestAlarm = alarmRepository.getLatestRecord()

        val armedUntilText = stateHolder.activeSession.value?.windowEndTs?.takeIf { it > 0 }?.let {
            TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it))
        }.orEmpty()

        val pendingText = if (pendingRemaining > 0) {
            TimeUtils.formatCountdown(Duration.ofMillis(pendingRemaining))
        } else {
            ""
        }

        val lastScreenOffText = if (lastScreenOffTs > 0) {
            val time = TimeUtils.toLocalTime(lastScreenOffTs)
            TimeUtils.formatAlarmTime(time)
        } else {
            ""
        }

        val latestAlarmText = if (latestAlarm != null) {
            val time = TimeUtils.toLocalTime(latestAlarm.triggerAt)
            "Alarm scheduled for ${TimeUtils.formatAlarmTime(time)}"
        } else {
            "No alarms scheduled yet"
        }

        val latestAlarmSubtitle = if (latestAlarm != null) {
            "Scheduled automatically"
        } else {
            ""
        }

        val notificationWarningText = if (!PermissionUtils.canPostNotifications(context)) {
            "Notifications disabled; lockscreen UI may be limited."
        } else {
            ""
        }

        val statusText = when (state) {
            AppState.DISARMED -> "Disarmed"
            AppState.ARMED_IDLE -> "Armed"
            AppState.ARMED_PENDING_CONFIRM -> "Confirming screen off"
            AppState.ARMED_ALARM_SET -> "Alarm created"
        }

        _uiState.value = MainUiState(
            armed = armed,
            statusText = statusText,
            armedUntilText = armedUntilText,
            lastScreenOffText = lastScreenOffText,
            latestAlarmText = latestAlarmText,
            latestAlarmSubtitle = latestAlarmSubtitle,
            notificationWarningText = notificationWarningText,
            pendingCountdownText = pendingText,
            showPending = pendingRemaining > 0
        )
    }

    fun toggleArmed() {
        viewModelScope.launch {
            if (armManager.isArmed()) {
                armManager.disarm()
            } else {
                armManager.arm(com.sleep8.domain.model.ArmSource.APP_BUTTON)
            }
            updateState()
        }
    }
}
