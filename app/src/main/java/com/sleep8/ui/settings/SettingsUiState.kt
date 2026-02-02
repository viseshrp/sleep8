package com.sleep8.ui.settings

data class SettingsUiState(
    val nightStart: String = "21:00",
    val nightEnd: String = "04:00",
    val autoArmStart: String = "20:00",
    val autoArmEnd: String = "04:00",
    val alarmDurationHoursInput: String = "8",
    val alarmDurationMinutesInput: String = "0",
    val alarmDurationError: String? = null,
    val confirmOffMinutes: String = "10",
    val armedDefault: Boolean = false,
    val autoArmEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val exactAlarmAllowed: Boolean = false,
    val batteryOptimizationsIgnored: Boolean = false,
    val foregroundServiceActive: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val overlayAllowed: Boolean = false
)
