package com.sleep8.ui.settings

data class SettingsUiState(
    val nightStart: String = "22:00",
    val nightEnd: String = "08:00",
    val autoArmStart: String = "22:00",
    val autoArmEnd: String = "08:00",
    val alarmDurationMinutesInput: String = "480",
    val alarmDurationError: String? = null,
    val confirmOffMinutes: String = "10",
    val snoozeEnabled: Boolean = false,
    val snoozeMinutes: String = "5",
    val armedDefault: Boolean = false,
    val autoArmEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val exactAlarmAllowed: Boolean = false,
    val batteryOptimizationsIgnored: Boolean = false,
    val foregroundServiceActive: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val overlayAllowed: Boolean = false
)
