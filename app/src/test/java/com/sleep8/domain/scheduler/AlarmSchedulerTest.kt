package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.domain.model.Settings
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmSchedulerTest {

    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
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
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmOffsetHours = 8,
            armedDefault = false,
            autoArmEnabled = true
        )
        scheduler = AlarmScheduler(context, alarmManager, alarmRepository, settingsRepository, prefs, notificationHelper)
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
                it.triggerAt == expectedTrigger &&
                    it.durationUsedMinutes == 480 &&
                    it.source == AlarmSource.SLEEP_AUTOMATION &&
                    it.status == AlarmStatus.SCHEDULED &&
                    it.scheduledViaAlarmClock
            })
        }
        val infoSlot = io.mockk.slot<AlarmManager.AlarmClockInfo>()
        verify { alarmManager.setAlarmClock(capture(infoSlot), any()) }
        assertEquals(expectedTrigger, infoSlot.captured.triggerTime)
    }

    @Test
    fun `schedule sleep alarm uses configured duration`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmOffsetHours = 6,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { alarmRepository.insertRecord(any()) } returns 2L
        val screenOff = Instant.parse("2024-01-15T23:30:00Z").toEpochMilli()

        scheduler.scheduleSleepAlarm(screenOff, confirmedAt = 1000L)

        val expectedTrigger = screenOff + 6 * 3600_000L
        coVerify {
            alarmRepository.insertRecord(match {
                it.triggerAt == expectedTrigger && it.durationUsedMinutes == 360
            })
        }
    }

    @Test
    fun `schedule sleep alarm creates unique instance ids`() = runTest {
        val screenOff = Instant.parse("2024-01-15T23:30:00Z").toEpochMilli()

        val records = mutableListOf<AlarmRecord>()
        coEvery { alarmRepository.insertRecord(capture(records)) } returnsMany listOf(3L, 4L)

        scheduler.scheduleSleepAlarm(screenOff, confirmedAt = 1000L)
        scheduler.scheduleSleepAlarm(screenOff + 1000L, confirmedAt = 2000L)

        assertEquals(2, records.size)
        assertNotEquals(records[0].alarmInstanceId, records[1].alarmInstanceId)
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
            durationUsedMinutes = 480,
            alarmInstanceId = 111L,
            requestCode = 111,
            scheduledViaAlarmClock = true,
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
        verify { alarmManager.setAlarmClock(any(), any()) }
    }
}
