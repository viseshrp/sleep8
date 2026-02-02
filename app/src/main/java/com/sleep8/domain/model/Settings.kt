package com.sleep8.domain.model

data class Settings(
    val nightStart: String,
    val nightEnd: String,
    val confirmOffMinutes: Int,
    val snoozeMinutes: Int?,
    val alarmDurationMinutes: Int,
    val overlayEnabled: Boolean,
    val armedDefault: Boolean,
    val autoArmEnabled: Boolean = false,
    val autoArmStart: String = nightStart,
    val autoArmEnd: String = nightEnd
)
