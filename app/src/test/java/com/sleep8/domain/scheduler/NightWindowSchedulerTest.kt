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
class NightWindowSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: NightWindowScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduler = NightWindowScheduler(context, alarmManager)
    }

    @Test
    fun `schedule night window end sets exact alarm`() {
        val triggerAt = System.currentTimeMillis() + 10_000L
        scheduler.scheduleWindowEnd(triggerAt)
        val scheduled = shadowOf(alarmManager).nextScheduledAlarm
        assertEquals(triggerAt, scheduled?.triggerAtTime)
    }

    @Test
    fun `cancel night window start removes alarm`() {
        scheduler.scheduleWindowStart(System.currentTimeMillis() + 10_000L)
        scheduler.cancelWindowStart()
        val shadow = shadowOf(alarmManager)
        assertTrue(shadow.scheduledAlarms.isEmpty())
    }
}
