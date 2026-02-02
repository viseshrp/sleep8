package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.util.PermissionUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmSchedulerTest {

    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val prefs = AppPreferences(InMemorySharedPreferences())
    private lateinit var scheduler: AlarmScheduler
    private lateinit var context: Context

    @Before
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext()
        prefs.activeSessionId = 42L
        mockkObject(PermissionUtils)
        every { PermissionUtils.canScheduleExactAlarms(any()) } returns true
        scheduler = AlarmScheduler(context, alarmManager, alarmRepository, prefs, notificationHelper)
    }

    @After
    fun tearDown() {
        unmockkObject(PermissionUtils)
    }

    @Test
    fun `schedule sleep alarm sets trigger at screen off plus 8 hours`() = runTest {
        coEvery { alarmRepository.insertRecord(any()) } returns 1L
        val screenOff = Instant.parse("2024-01-15T23:30:00Z").toEpochMilli()

        scheduler.scheduleSleepAlarm(screenOff, confirmedAt = 1000L)

        val expectedTrigger = screenOff + 8 * 3600_000L
        coVerify {
            alarmRepository.insertRecord(match {
                it.triggerAt == expectedTrigger && it.source == AlarmSource.SLEEP_AUTOMATION && it.status == AlarmStatus.SCHEDULED
            })
        }
        verify { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expectedTrigger, any()) }
    }

    @Test
    fun `schedule snooze updates original and schedules new alarm`() = runTest {
        val original = AlarmRecord(
            id = 5L,
            sessionId = 42L,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.FIRED,
            firedAt = 3000L,
            dismissedAt = null,
            snoozedUntil = null
        )
        coEvery { alarmRepository.getRecord(5L) } returns original
        coEvery { alarmRepository.insertRecord(any()) } returns 6L

        scheduler.scheduleSnooze(5L, snoozeMinutes = 10)

        coVerify { alarmRepository.markSnoozed(5L, any()) }
        verify { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
    }
}
