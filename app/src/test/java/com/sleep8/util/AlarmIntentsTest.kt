package com.sleep8.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
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
