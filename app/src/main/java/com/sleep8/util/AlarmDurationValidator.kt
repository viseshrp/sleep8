package com.sleep8.util

object AlarmDurationValidator {

    fun clamp(value: Int): Int {
        return value.coerceIn(Constants.ALARM_MIN_DURATION_MINUTES, Constants.ALARM_MAX_DURATION_MINUTES)
    }

    fun parseMinutes(raw: String?): Int? {
        return raw?.trim()?.toIntOrNull()
    }

    fun errorFor(raw: String?): String? {
        val value = parseMinutes(raw) ?: return "Enter a value between 0 and 720 minutes."
        return if (value < Constants.ALARM_MIN_DURATION_MINUTES || value > Constants.ALARM_MAX_DURATION_MINUTES) {
            "Enter a value between 0 and 720 minutes."
        } else {
            null
        }
    }
}
