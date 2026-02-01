package com.sleep8.ui.settings

data class SettingsUiState(
    val nightStart: String = "22:00",
    val nightEnd: String = "08:00",
    val alarmOffsetHours: String = "8",
    val confirmOffMinutes: String = "10",
    val snoozeEnabled: Boolean = false,
    val snoozeMinutes: String = "5",
    val armedDefault: Boolean = false,
    val offlineOnly: Boolean = true,
    val exactAlarmAllowed: Boolean = false,
    val batteryOptimizationsIgnored: Boolean = false,
    val foregroundServiceActive: Boolean = false
)
