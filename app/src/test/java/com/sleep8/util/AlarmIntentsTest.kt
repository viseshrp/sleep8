package com.sleep8.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test
    fun `parse alarm id ignores unrelated hosts`() {
        val uri = android.net.Uri.parse("sleep8://alarms/42")
        assertNull(AlarmIntents.parseAlarmId(uri))
    }

    @Test
    fun `alarm history and detail intents target this package`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val historyIntent = AlarmIntents.alarmHistoryIntent(context)
        val detailIntent = AlarmIntents.alarmDetailIntent(context, 77L)

        assertEquals(context.packageName, historyIntent.`package`)
        assertEquals(context.packageName, detailIntent.`package`)
        assertEquals("sleep8://alarm/77", detailIntent.dataString)
    }

    @Test
    fun `pending intent builders return non null intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val historyPendingIntent = AlarmIntents.alarmHistoryPendingIntent(context, 101)
        val detailPendingIntent = AlarmIntents.alarmDetailPendingIntent(context, 102, 55L)

        assertNotNull(historyPendingIntent)
        assertNotNull(detailPendingIntent)
    }
}
