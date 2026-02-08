package com.sleep8.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.MonitoringReliabilityManager
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val armManager: ArmManager,
    private val monitoringReliabilityManager: MonitoringReliabilityManager,
    private val stateHolder: StateHolder,
    private val alarmRepository: com.sleep8.data.repository.AlarmRepository,
    private val settingsRepository: com.sleep8.data.repository.SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val _startupReady = MutableStateFlow(false)
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()
    private var latestAlarm: com.sleep8.domain.model.AlarmRecord? = null
    private var latestAlarmRefreshAt: Long = 0L

    init {
        viewModelScope.launch {
            monitoringReliabilityManager.reconcileOnForeground(context)
            updateState(forceRefreshLatestAlarm = true)
            _startupReady.value = true
            while (isActive) {
                delay(1000)
                updateState()
            }
        }
    }

    private suspend fun updateState(forceRefreshLatestAlarm: Boolean = false) {
        val state = stateHolder.state.value
        val armed = state != AppState.DISARMED
        val lastScreenOffTs = stateHolder.lastScreenOffTs.value
        val pendingDeadline = stateHolder.pendingConfirmDeadlineTs.value
        val now = System.currentTimeMillis()
        val pendingRemaining = if (pendingDeadline > 0) pendingDeadline - now else 0L
        if (forceRefreshLatestAlarm || latestAlarm == null || now - latestAlarmRefreshAt >= LATEST_ALARM_REFRESH_MS) {
            latestAlarm = alarmRepository.getLatestScheduledRecord()
            latestAlarmRefreshAt = now
        }

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

        val latestAlarmRecord = latestAlarm
        val latestAlarmText = if (latestAlarmRecord != null) {
            val time = TimeUtils.toLocalTime(latestAlarmRecord.triggerAt)
            "Alarm scheduled for ${TimeUtils.formatAlarmTime(time)}"
        } else {
            ""
        }

        val latestAlarmSubtitle = if (latestAlarmRecord != null) {
            "Scheduled automatically"
        } else {
            ""
        }

        val notificationWarningText = if (!PermissionUtils.canPostNotifications(context)) {
            "Notifications disabled; lockscreen UI may be limited."
        } else {
            ""
        }
        val settings = settingsRepository.getSettings()
        val inNightWindow = TimeUtils.isInWindow(
            LocalDateTime.now().toLocalTime(),
            TimeUtils.parseLocalTime(settings.nightStart),
            TimeUtils.parseLocalTime(settings.nightEnd)
        )
        val monitoringActive = PermissionUtils.isServiceRunning(context, com.sleep8.service.NightMonitorService::class.java)
        val monitoringHealthText = when {
            !armed || !inNightWindow -> "Healthy (not required now)"
            monitoringActive -> "Healthy (monitoring active)"
            else -> "Degraded (monitoring should be active)"
        }
        val latestReason = monitoringReliabilityManager.latestReasonLabel()
        val reliabilityWarningText = when (latestReason) {
            "app restricted / force-stopped suspected" -> "Pixel guidance: disable Extreme Battery Saver for Sleep8 and set App battery usage to Unrestricted. If the app was force-stopped, open Sleep8 once to re-enable background starts."
            "process not started" -> "Sleep8 recovered late. On Pixel, set App battery usage to Unrestricted to reduce missed starts."
            "boundary event did not run" -> "Night window boundary trigger was missed. Sleep8 will keep retrying via backstops and health checks."
            "start attempt blocked" -> "Android blocked a background start attempt. Allow Exact alarms and keep Sleep8 out of restrictive battery modes."
            else -> ""
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
            monitoringHealthText = monitoringHealthText,
            reliabilityWarningText = reliabilityWarningText,
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
            updateState(forceRefreshLatestAlarm = true)
        }
    }

    private companion object {
        const val LATEST_ALARM_REFRESH_MS = 30_000L
    }
}
