package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ConfirmOffSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: ConfirmOffScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduler = ConfirmOffScheduler(context, alarmManager, AppPreferences(InMemorySharedPreferences()))
    }

    @Test
    fun `schedule confirmation sets exact alarm at correct time`() {
        val screenOffTime = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(screenOffTime, 10)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduled = shadowAlarmManager.nextScheduledAlarm
        assertTrue(scheduled.triggerAtTime in (screenOffTime + 600_000L - 1_000L)..(screenOffTime + 600_000L + 1_000L))
    }

    @Test
    fun `reschedule confirmation cancels previous and sets new`() {
        val firstOff = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(firstOff, 10)
        val secondOff = firstOff + 120_000L
        scheduler.scheduleConfirmation(secondOff, 10)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduled = shadowAlarmManager.nextScheduledAlarm
        assertEquals(secondOff + 600_000L, scheduled.triggerAtTime)
    }

    @Test
    fun `cancel confirmation removes pending alarm`() {
        val screenOffTime = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(screenOffTime, 10)
        scheduler.cancelConfirmation()

        val shadowAlarmManager = shadowOf(alarmManager)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `schedule with custom confirm minutes uses setting`() {
        val screenOffTime = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(screenOffTime, 5)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduled = shadowAlarmManager.nextScheduledAlarm
        assertEquals(screenOffTime + 300_000L, scheduled.triggerAtTime)
    }
}
