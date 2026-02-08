package com.sleep8.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AlarmDurationValidatorTest {

    @Test
    fun `clamp enforces bounds`() {
        assertEquals(0, AlarmDurationValidator.clamp(-5))
        assertEquals(720, AlarmDurationValidator.clamp(999))
        assertEquals(0, AlarmDurationValidator.clamp(0))
        assertEquals(720, AlarmDurationValidator.clamp(720))
    }

    @Test
    fun `normalize rejects invalid inputs`() {
        val result = AlarmDurationValidator.normalizeInputs("", "0")
        assertNotNull(result.error)
    }

    @Test
    fun `normalize rejects negative inputs`() {
        val result = AlarmDurationValidator.normalizeInputs("-1", "5")
        assertNotNull(result.error)
        assertEquals("Enter a value between 0 and 720 minutes.", result.error)
    }

    @Test
    fun `normalizes minutes into hours`() {
        val result = AlarmDurationValidator.normalizeInputs("1", "75")
        assertNull(result.error)
        assertEquals("2", result.hoursInput)
        assertEquals("15", result.minutesInput)
    }

    @Test
    fun `clamps total to max`() {
        val result = AlarmDurationValidator.normalizeInputs("12", "30")
        assertNotNull(result.error)
        assertEquals("12", result.hoursInput)
        assertEquals("0", result.minutesInput)
        assertEquals(720, result.totalMinutes)
    }

    @Test
    fun `converts hours and minutes to total`() {
        assertEquals(480, AlarmDurationValidator.normalizeInputs("8", "0").totalMinutes)
        assertEquals(0, AlarmDurationValidator.normalizeInputs("0", "0").totalMinutes)
        assertEquals(720, AlarmDurationValidator.normalizeInputs("12", "0").totalMinutes)
    }
}
