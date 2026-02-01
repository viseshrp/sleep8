package com.sleep8.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.AppState
import com.sleep8.domain.state.StateHolder
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
    private val stateHolder: StateHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.value = _uiState.value.copy(
                nightStart = settings.nightStart,
                nightEnd = settings.nightEnd,
                alarmOffsetHours = settings.alarmOffsetHours.toString(),
                confirmOffMinutes = settings.confirmOffMinutes.toString(),
                snoozeEnabled = settings.snoozeMinutes != null,
                snoozeMinutes = settings.snoozeMinutes?.toString() ?: "5",
                armedDefault = settings.armedDefault
            )
        }
    }

    fun refreshReliability(context: Context) {
        val exactAllowed = PermissionUtils.canScheduleExactAlarms(context)
        val batteryIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context)
        val isServiceRunning = PermissionUtils.isServiceRunning(context, NightMonitorService::class.java)
        
        _uiState.value = _uiState.value.copy(
            exactAlarmAllowed = exactAllowed,
            batteryOptimizationsIgnored = batteryIgnored,
            foregroundServiceActive = isServiceRunning
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

    fun updateAlarmOffset(value: String) {
        _uiState.value = _uiState.value.copy(alarmOffsetHours = value)
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

    private fun persist() {
        viewModelScope.launch {
            val state = _uiState.value
            val snooze = state.snoozeMinutes.toIntOrNull()
            val offset = state.alarmOffsetHours.toIntOrNull() ?: 8
            val confirmOff = state.confirmOffMinutes.toIntOrNull() ?: 10
            val settings = Settings(
                nightStart = state.nightStart,
                nightEnd = state.nightEnd,
                confirmOffMinutes = confirmOff,
                snoozeMinutes = if (state.snoozeEnabled) snooze else null,
                alarmOffsetHours = offset,
                armedDefault = state.armedDefault
            )
            settingsRepository.updateSettings(settings)
        }
    }

    fun setBatteryOptAck(ack: Boolean) {
        appPreferences.batteryOptOutAck = ack
    }
}
