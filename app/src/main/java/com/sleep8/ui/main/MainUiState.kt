package com.sleep8.ui.main

data class MainUiState(
    val armed: Boolean = false,
    val statusText: String = "",
    val windowEndsText: String = "",
    val lastScreenOffText: String = "",
    val latestAlarmText: String = "",
    val latestAlarmSubtitle: String = "",
    val notificationWarningText: String = "",
    val monitoringHealthText: String = "",
    val reliabilityWarningText: String = "",
    val pendingCountdownText: String = "",
    val showPending: Boolean = false
)
