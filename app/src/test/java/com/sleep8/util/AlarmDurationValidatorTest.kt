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
    fun `error returned for invalid values`() {
        assertNotNull(AlarmDurationValidator.errorFor("-1"))
        assertNotNull(AlarmDurationValidator.errorFor("721"))
        assertNotNull(AlarmDurationValidator.errorFor(""))
    }

    @Test
    fun `no error for valid values`() {
        assertNull(AlarmDurationValidator.errorFor("0"))
        assertNull(AlarmDurationValidator.errorFor("1"))
        assertNull(AlarmDurationValidator.errorFor("480"))
        assertNull(AlarmDurationValidator.errorFor("720"))
    }
}
