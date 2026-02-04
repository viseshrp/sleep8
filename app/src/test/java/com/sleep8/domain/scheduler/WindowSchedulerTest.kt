package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class WindowSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: WindowScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduler = WindowScheduler(context, alarmManager)
    }

    @Test
    fun `schedule window start sets exact alarm`() {
        val triggerAt = System.currentTimeMillis() + 5_000L
        scheduler.scheduleWindowStart(triggerAt)
        val scheduled = shadowOf(alarmManager).nextScheduledAlarm
        assertEquals(triggerAt, scheduled?.triggerAtTime)
    }

    @Test
    fun `cancel window end removes scheduled alarm`() {
        scheduler.scheduleWindowEnd(System.currentTimeMillis() + 5_000L)
        scheduler.cancelWindowEnd()
        val shadow = shadowOf(alarmManager)
        assertTrue(shadow.scheduledAlarms.isEmpty())
    }
}
