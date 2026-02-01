package com.sleep8.ui.settings

data class SettingsUiState(
    val nightStart: String = "22:00",
    val nightEnd: String = "08:00",
    val snoozeEnabled: Boolean = false,
    val snoozeMinutes: String = "",
    val exactAlarmAllowed: Boolean = false,
    val batteryOptimizationsIgnored: Boolean = false,
    val foregroundServiceActive: Boolean = false
)
