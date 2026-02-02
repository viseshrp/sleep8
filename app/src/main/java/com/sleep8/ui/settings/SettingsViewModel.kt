package com.sleep8.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.model.Settings
import com.sleep8.service.NightMonitorService
import com.sleep8.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appPreferences: AppPreferences,
    private val armManager: ArmManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.value = _uiState.value.copy(
                nightStart = settings.nightStart,
                nightEnd = settings.nightEnd,
                autoArmStart = settings.autoArmStart,
                autoArmEnd = settings.autoArmEnd,
                alarmDurationMinutesInput = settings.alarmDurationMinutes.toString(),
                alarmDurationError = null,
                confirmOffMinutes = settings.confirmOffMinutes.toString(),
                snoozeEnabled = settings.snoozeMinutes != null,
                snoozeMinutes = settings.snoozeMinutes?.toString() ?: "5",
                armedDefault = settings.armedDefault,
                autoArmEnabled = settings.autoArmEnabled,
                overlayEnabled = settings.overlayEnabled
            )
            appPreferences.alarmDurationMinutes = settings.alarmDurationMinutes
        }
    }

    fun refreshReliability(context: Context) {
        val exactAllowed = PermissionUtils.canScheduleExactAlarms(context)
        val batteryIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context)
        val isServiceRunning = PermissionUtils.isServiceRunning(context, NightMonitorService::class.java)
        val notificationsAllowed = PermissionUtils.canPostNotifications(context)
        val overlayAllowed = PermissionUtils.canDrawOverlays(context)
        
        _uiState.value = _uiState.value.copy(
            exactAlarmAllowed = exactAllowed,
            batteryOptimizationsIgnored = batteryIgnored,
            foregroundServiceActive = isServiceRunning,
            notificationsAllowed = notificationsAllowed,
            overlayAllowed = overlayAllowed
        )
    }

    fun updateNightStart(value: String) {
        _uiState.value = _uiState.value.copy(nightStart = value)
        persist()
    }

    fun updateNightEnd(value: String) {
        _uiState.value = _uiState.value.copy(nightEnd = value)
        persist()
    }

    fun updateAutoArmStart(value: String) {
        _uiState.value = _uiState.value.copy(autoArmStart = value)
        if (_uiState.value.autoArmEnabled) {
            rescheduleAutoArm()
        } else {
            persist()
        }
    }

    fun updateAutoArmEnd(value: String) {
        _uiState.value = _uiState.value.copy(autoArmEnd = value)
        if (_uiState.value.autoArmEnabled) {
            rescheduleAutoArm()
        } else {
            persist()
        }
    }

    fun updateAlarmDurationMinutes(value: String) {
        _uiState.value = _uiState.value.copy(
            alarmDurationMinutesInput = value,
            alarmDurationError = com.sleep8.util.AlarmDurationValidator.errorFor(value)
        )
        persist()
    }

    fun updateConfirmOffMinutes(value: String) {
        _uiState.value = _uiState.value.copy(confirmOffMinutes = value)
        persist()
    }

    fun updateSnooze(enabled: Boolean, minutes: String) {
        _uiState.value = _uiState.value.copy(snoozeEnabled = enabled, snoozeMinutes = minutes)
        persist()
    }

    fun updateArmedDefault(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(armedDefault = enabled)
        persist()
    }

    fun updateAutoArmEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoArmEnabled = enabled)
        viewModelScope.launch {
            persistSettings(_uiState.value)
            armManager.updateAutoArmEnabled(enabled)
        }
    }

    fun updateOverlayEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(overlayEnabled = enabled)
        persist()
    }

    private fun persist() {
        viewModelScope.launch {
            persistSettings(_uiState.value)
        }
    }

    private fun rescheduleAutoArm() {
        viewModelScope.launch {
            persistSettings(_uiState.value)
            armManager.updateAutoArmEnabled(true)
        }
    }

    private suspend fun persistSettings(state: SettingsUiState) {
        val snooze = state.snoozeMinutes.toIntOrNull()
        val isDurationValid = com.sleep8.util.AlarmDurationValidator.errorFor(state.alarmDurationMinutesInput) == null
        val durationMinutes = if (isDurationValid) {
            com.sleep8.util.AlarmDurationValidator.clamp(
                com.sleep8.util.AlarmDurationValidator.parseMinutes(state.alarmDurationMinutesInput)
                    ?: com.sleep8.util.Constants.ALARM_DEFAULT_DURATION_MINUTES
            )
        } else {
            appPreferences.alarmDurationMinutes
        }
        val confirmOff = state.confirmOffMinutes.toIntOrNull() ?: 10
        val settings = Settings(
            nightStart = state.nightStart,
            nightEnd = state.nightEnd,
            confirmOffMinutes = confirmOff,
            snoozeMinutes = if (state.snoozeEnabled) snooze else null,
            alarmDurationMinutes = durationMinutes,
            overlayEnabled = state.overlayEnabled,
            armedDefault = state.armedDefault,
            autoArmEnabled = state.autoArmEnabled,
            autoArmStart = state.autoArmStart,
            autoArmEnd = state.autoArmEnd
        )
        settingsRepository.updateSettings(settings)
        if (isDurationValid) {
            appPreferences.alarmDurationMinutes = durationMinutes
        }
    }

    fun setBatteryOptAck(ack: Boolean) {
        appPreferences.batteryOptOutAck = ack
    }

    fun setNotificationsAsked() {
        appPreferences.notificationsAsked = true
    }
}
