package com.sleep8.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AlarmIntentsTest {

    @Test
    fun `alarm detail uri parses alarm id`() {
        val uri = AlarmIntents.alarmDetailUri(42L)
        assertEquals(42L, AlarmIntents.parseAlarmId(uri))
    }

    @Test
    fun `alarm history uri does not parse as alarm id`() {
        val uri = AlarmIntents.alarmHistoryUri()
        assertNull(AlarmIntents.parseAlarmId(uri))
    }
}
