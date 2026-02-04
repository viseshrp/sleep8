package com.sleep8.ui.settings

import com.sleep8.util.Constants

data class SettingsUiState(
    val nightStart: String = Constants.DEFAULT_NIGHT_START,
    val nightEnd: String = Constants.DEFAULT_NIGHT_END,
    val autoArmStart: String = Constants.DEFAULT_AUTO_ARM_START,
    val autoArmEnd: String = Constants.DEFAULT_AUTO_ARM_END,
    val alarmDurationHoursInput: String = "8",
    val alarmDurationMinutesInput: String = "0",
    val alarmDurationError: String? = null,
    val confirmOffMinutes: String = Constants.DEFAULT_CONFIRM_MINUTES.toString(),
    val armedDefault: Boolean = false,
    val autoArmEnabled: Boolean = false,
    val darkModeEnabled: Boolean = true,
    val overlayEnabled: Boolean = false,
    val exactAlarmAllowed: Boolean = false,
    val batteryOptimizationsIgnored: Boolean = false,
    val foregroundServiceActive: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val overlayAllowed: Boolean = false
)
