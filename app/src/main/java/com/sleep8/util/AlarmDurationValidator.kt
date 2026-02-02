package com.sleep8.util

object AlarmDurationValidator {

    fun clamp(value: Int): Int {
        return value.coerceIn(Constants.ALARM_MIN_DURATION_MINUTES, Constants.ALARM_MAX_DURATION_MINUTES)
    }

    fun split(totalMinutes: Int): Pair<Int, Int> {
        val clamped = clamp(totalMinutes)
        return clamped / 60 to clamped % 60
    }

    data class DurationResult(
        val hoursInput: String,
        val minutesInput: String,
        val totalMinutes: Int?,
        val error: String?
    )

    fun normalizeInputs(hoursRaw: String, minutesRaw: String): DurationResult {
        val hours = hoursRaw.trim().toIntOrNull()
        val minutes = minutesRaw.trim().toIntOrNull()
        if (hours == null || minutes == null) {
            return DurationResult(hoursRaw, minutesRaw, null, "Enter hours and minutes.")
        }
        if (hours < 0 || minutes < 0) {
            return DurationResult(hoursRaw, minutesRaw, null, "Enter a value between 0 and 720 minutes.")
        }
        val total = hours * 60 + minutes
        val normalizedHours = total / 60
        val normalizedMinutes = total % 60
        val clampedTotal = clamp(total)
        val (clampedHours, clampedMinutes) = split(clampedTotal)
        val error = if (total != clampedTotal) {
            "Enter a value between 0 and 720 minutes."
        } else {
            null
        }
        return DurationResult(
            hoursInput = clampedHours.toString(),
            minutesInput = clampedMinutes.toString(),
            totalMinutes = clampedTotal,
            error = error
        )
    }
}
