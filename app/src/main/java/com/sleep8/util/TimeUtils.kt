package com.sleep8.util

import com.sleep8.domain.model.NightWindow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    fun initDefaultZone() {
        // No-op: always use current system ZoneId in calculations.
    }

    fun parseLocalTime(value: String): LocalTime {
        return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"))
    }

    fun isInWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (!crossesMidnight(start, end)) {
            now >= start && now <= end
        } else {
            now >= start || now <= end
        }
    }

    fun calculateNightWindow(now: LocalDateTime, start: LocalTime, end: LocalTime): NightWindow {
        val today = now.toLocalDate()
        val crosses = crossesMidnight(start, end)

        val startDate = if (crosses && now.toLocalTime().isBefore(end)) {
            today.minusDays(1)
        } else {
            today
        }
        val endDate = if (crosses && now.toLocalTime().isAfter(start)) {
            today.plusDays(1)
        } else if (crosses && now.toLocalTime().isBefore(end)) {
            today
        } else {
            today
        }

        val startDateTime = LocalDateTime.of(startDate, start)
        val endDateTime = LocalDateTime.of(endDate, end)

        val zoneId = ZoneId.systemDefault()
        return NightWindow(
            startTs = startDateTime.atZone(zoneId).toInstant().toEpochMilli(),
            endTs = endDateTime.atZone(zoneId).toInstant().toEpochMilli()
        )
    }

    fun calculateNextWindow(now: LocalDateTime, start: LocalTime, end: LocalTime): NightWindow {
        return if (isInWindow(now.toLocalTime(), start, end)) {
            calculateNightWindow(now, start, end)
        } else {
            val today = now.toLocalDate()
            val startDate = if (now.toLocalTime().isBefore(start)) today else today.plusDays(1)
            val endDate = if (crossesMidnight(start, end)) startDate.plusDays(1) else startDate
            val startDateTime = LocalDateTime.of(startDate, start)
            val endDateTime = LocalDateTime.of(endDate, end)
            val zoneId = ZoneId.systemDefault()
            NightWindow(
                startTs = startDateTime.atZone(zoneId).toInstant().toEpochMilli(),
                endTs = endDateTime.atZone(zoneId).toInstant().toEpochMilli()
            )
        }
    }

    fun calculateAlarmTime(screenOff: Instant, offsetHours: Int): Instant {
        return screenOff.plus(Duration.ofHours(offsetHours.toLong()))
    }

    fun calculateRemainingConfirmTime(screenOff: Instant, confirmMinutes: Int): Duration {
        val deadline = screenOff.plus(Duration.ofMinutes(confirmMinutes.toLong()))
        return Duration.between(Instant.now(), deadline)
    }

    fun formatAlarmTime(time: LocalTime): String {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        return time.format(formatter)
    }

    fun formatCountdown(remaining: Duration): String {
        val totalSeconds = remaining.seconds.coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    fun toLocalTime(epochMillis: Long): LocalTime {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    }

    fun toLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    private fun crossesMidnight(start: LocalTime, end: LocalTime): Boolean {
        return start.isAfter(end)
    }
}
