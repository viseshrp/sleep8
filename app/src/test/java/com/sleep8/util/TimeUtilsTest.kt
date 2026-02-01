package com.sleep8.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class TimeUtilsTest {

    @Test
    fun `calculate alarm time adds 8 hours`() {
        val screenOff = Instant.parse("2024-01-15T23:00:00Z")
        val alarm = TimeUtils.calculateAlarmTime(screenOff, 8)
        assertEquals(Instant.parse("2024-01-16T07:00:00Z"), alarm)
    }

    @Test
    fun `calculate alarm time handles day rollover`() {
        val screenOff = Instant.parse("2024-01-15T20:00:00Z")
        val alarm = TimeUtils.calculateAlarmTime(screenOff, 8)
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
}
