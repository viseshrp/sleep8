package com.sleep8.ui.alarm

data class AlarmListItem(
    val id: Long,
    val timeText: String,
    val subtitle: String,
    val enabled: Boolean,
    val toggleEnabled: Boolean
)
