package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class ConfirmOffSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: ConfirmOffScheduler
    private lateinit var prefs: AppPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs = AppPreferences(InMemorySharedPreferences())
        scheduler = ConfirmOffScheduler(context, alarmManager, prefs)
    }

    @Test
    fun `schedule confirmation sets exact alarm at correct time`() {
        val screenOffTime = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(screenOffTime, 10)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduled = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(scheduled)
        assertTrue(scheduled!!.triggerAtTime in (screenOffTime + 600_000L - 1_000L)..(screenOffTime + 600_000L + 1_000L))
    }

    @Test
    fun `reschedule confirmation cancels previous and sets new`() {
        val firstOff = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(firstOff, 10)
        val secondOff = firstOff + 120_000L
        scheduler.scheduleConfirmation(secondOff, 10)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduled = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(scheduled)
        assertEquals(secondOff + 600_000L, scheduled!!.triggerAtTime)
    }

    @Test
    fun `cancel confirmation removes pending alarm`() {
        val screenOffTime = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(screenOffTime, 10)
        scheduler.cancelConfirmation()

        val shadowAlarmManager = shadowOf(alarmManager)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
        assertEquals(-1L, prefs.pendingCandidateScreenOffTs)
        assertEquals(-1L, prefs.pendingConfirmDeadlineTs)
    }

    @Test
    fun `schedule with custom confirm minutes uses setting`() {
        val screenOffTime = Instant.now().toEpochMilli()
        scheduler.scheduleConfirmation(screenOffTime, 5)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduled = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(scheduled)
        assertEquals(screenOffTime + 300_000L, scheduled!!.triggerAtTime)
    }

    @Test
    fun `schedule confirmation at stores pending timestamps`() {
        val screenOff = Instant.now().toEpochMilli()
        val deadline = screenOff + 90_000L

        scheduler.scheduleConfirmationAt(screenOff, deadline)

        assertEquals(screenOff, prefs.pendingCandidateScreenOffTs)
        assertEquals(deadline, prefs.pendingConfirmDeadlineTs)
    }

    @Test
    fun `cancel timer only keeps pending timestamps`() {
        val screenOff = Instant.now().toEpochMilli()
        val deadline = screenOff + 90_000L
        scheduler.scheduleConfirmationAt(screenOff, deadline)

        scheduler.cancelConfirmationTimerOnly()

        assertEquals(screenOff, prefs.pendingCandidateScreenOffTs)
        assertEquals(deadline, prefs.pendingConfirmDeadlineTs)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }
}
