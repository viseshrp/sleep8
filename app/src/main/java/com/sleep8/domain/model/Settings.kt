package com.sleep8.domain.model

data class Settings(
    val nightStart: String,
    val nightEnd: String,
    val confirmOffMinutes: Int,
    val alarmDurationMinutes: Int,
    val overlayEnabled: Boolean,
    val armedDefault: Boolean
)
