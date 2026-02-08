package com.sleep8.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

class TimeUtilsTest {

    @Test
    fun `calculate alarm time adds 8 hours`() {
        val screenOff = Instant.parse("2024-01-15T23:00:00Z")
        val alarm = TimeUtils.calculateAlarmTime(screenOff, 8 * 60)
        assertEquals(Instant.parse("2024-01-16T07:00:00Z"), alarm)
    }

    @Test
    fun `calculate alarm time handles day rollover`() {
        val screenOff = Instant.parse("2024-01-15T20:00:00Z")
        val alarm = TimeUtils.calculateAlarmTime(screenOff, 8 * 60)
        assertEquals(Instant.parse("2024-01-16T04:00:00Z"), alarm)
    }

    @Test
    fun `calculate remaining confirmation time - partial elapsed`() {
        val screenOff = Instant.now().minusSeconds(300)
        val remaining = TimeUtils.calculateRemainingConfirmTime(screenOff, 10)
        assertTrue(remaining.toMinutes() in 4..5)
    }

    @Test
    fun `calculate remaining confirmation time - fully elapsed`() {
        val screenOff = Instant.now().minusSeconds(700)
        val remaining = TimeUtils.calculateRemainingConfirmTime(screenOff, 10)
        assertTrue(remaining.isNegative || remaining.isZero)
    }

    @Test
    fun `format alarm time for display`() {
        val time = LocalTime.of(7, 30)
        val formatted = TimeUtils.formatAlarmTime(time)
        assertEquals("7:30 AM", formatted)
    }

    @Test
    fun `format countdown timer`() {
        val remaining = Duration.ofSeconds(325)
        val formatted = TimeUtils.formatCountdown(remaining)
        assertEquals("5:25", formatted)
    }

    @Test
    fun `format duration minutes`() {
        assertEquals("8h", TimeUtils.formatDurationMinutes(480))
        assertEquals("1h 30m", TimeUtils.formatDurationMinutes(90))
        assertEquals("45m", TimeUtils.formatDurationMinutes(45))
    }

    @Test
    fun `init default zone is a safe no-op`() {
        TimeUtils.initDefaultZone()
    }

    @Test
    fun `to local date time converts epoch millis`() {
        val epoch = Instant.parse("2024-01-15T23:45:00Z").toEpochMilli()
        val local = TimeUtils.toLocalDateTime(epoch)
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), java.time.ZoneId.systemDefault()), local)
    }

    @Test
    fun `calculate night window handles crossing midnight for early morning`() {
        val now = LocalDateTime.of(2024, 1, 16, 1, 0)
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)

        val window = TimeUtils.calculateNightWindow(now, start, end)
        val zone = java.time.ZoneId.systemDefault()
        val expectedStart = LocalDateTime.of(2024, 1, 15, 22, 0).atZone(zone).toInstant().toEpochMilli()
        val expectedEnd = LocalDateTime.of(2024, 1, 16, 8, 0).atZone(zone).toInstant().toEpochMilli()

        assertEquals(expectedStart, window.startTs)
        assertEquals(expectedEnd, window.endTs)
    }

    @Test
    fun `calculate next window outside crossing-midnight window schedules tonight`() {
        val now = LocalDateTime.of(2024, 1, 16, 12, 0)
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)

        val window = TimeUtils.calculateNextWindow(now, start, end)
        val zone = java.time.ZoneId.systemDefault()
        val expectedStart = LocalDateTime.of(2024, 1, 16, 22, 0).atZone(zone).toInstant().toEpochMilli()
        val expectedEnd = LocalDateTime.of(2024, 1, 17, 8, 0).atZone(zone).toInstant().toEpochMilli()

        assertEquals(expectedStart, window.startTs)
        assertEquals(expectedEnd, window.endTs)
    }
}
