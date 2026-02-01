package com.sleep8.domain.model

data class Settings(
    val nightStart: String,
    val nightEnd: String,
    val confirmOffMinutes: Int,
    val snoozeMinutes: Int?,
    val alarmOffsetHours: Int,
    val armedDefault: Boolean,
    val autoArmEnabled: Boolean = false // new property for auto-arm
)
