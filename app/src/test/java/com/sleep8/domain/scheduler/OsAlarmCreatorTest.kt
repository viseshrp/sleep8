package com.sleep8.domain.scheduler

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.AlarmClock
import com.sleep8.R
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.Settings
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import java.time.Instant
import java.util.TimeZone

class OsAlarmCreatorTest {

    @BeforeEach
    fun setupTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private val settingsRepository = mockk<SettingsRepository>()
    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
    private val backstopScheduler = mockk<BackstopAlarmScheduler>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val prefs = AppPreferences(InMemorySharedPreferences())

    private val packageManager = mockk<PackageManager>()
    private val context = mockk<Context>(relaxed = true) {
        every { this@mockk.packageManager } returns packageManager
        every { this@mockk.getString(R.string.alarm_message) } returns "Sleep8 Alarm"
    }

    private val creator = OsAlarmCreator(context, settingsRepository, alarmRepository, backstopScheduler, prefs, notificationHelper)

    @Test
    fun `create alarm sets correct hour and minute`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmOffsetHours = 8,
            armedDefault = false,
            autoArmEnabled = true
        )
        every { packageManager.resolveActivity(any(), any()) } returns ResolveInfo()

        val screenOffTime = Instant.parse("2024-01-15T23:30:00Z").toEpochMilli()
        creator.createAlarm(screenOffTime)

        verify {
            context.startActivity(match { intent: Intent ->
                intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1) == 7 &&
                    intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1) == 30 &&
                    intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true) == false
            })
        }
    }

    @Test
    fun `create alarm includes snooze when configured`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = 10,
            alarmOffsetHours = 8,
            armedDefault = false,
            autoArmEnabled = true
        )
        every { packageManager.resolveActivity(any(), any()) } returns ResolveInfo()

        creator.createAlarm(Instant.parse("2024-01-15T23:30:00Z").toEpochMilli())

        verify {
            context.startActivity(match { intent: Intent ->
                intent.getIntExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, -1) == 10 &&
                    intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true) == false
            })
        }
    }

    @Test
    fun `create alarm always shows clock ui`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmOffsetHours = 8,
            armedDefault = false,
            autoArmEnabled = true
        )
        every { packageManager.resolveActivity(any(), any()) } returns ResolveInfo()

        creator.createAlarm(Instant.parse("2024-01-15T23:30:00Z").toEpochMilli())

        verify {
            context.startActivity(match { intent: Intent ->
                intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true) == false
            })
        }
    }

    @Test
    fun `create alarm records success in database`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmOffsetHours = 8,
            armedDefault = false,
            autoArmEnabled = true
        )
        every { packageManager.resolveActivity(any(), any()) } returns ResolveInfo()

        val result = creator.createAlarm(Instant.parse("2024-01-15T23:30:00Z").toEpochMilli())

        assertTrue(result is AlarmCreationResult.Success)
        coVerify { alarmRepository.insertRecord(any()) }
    }

    @Test
    fun `create alarm handles non-resolving intent`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmOffsetHours = 8,
            armedDefault = false,
            autoArmEnabled = true
        )
        every { packageManager.resolveActivity(any(), any()) } returns null

        val result = creator.createAlarm(Instant.parse("2024-01-15T23:30:00Z").toEpochMilli()) as AlarmCreationResult.Success
        assertFalse(result.record.osAlarmIntentResolved)
    }

    @Test
    fun `create alarm schedules backstop`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmOffsetHours = 8,
            armedDefault = false,
            autoArmEnabled = true
        )
        every { packageManager.resolveActivity(any(), any()) } returns ResolveInfo()

        creator.createAlarm(Instant.parse("2024-01-15T23:30:00Z").toEpochMilli())

        verify { backstopScheduler.scheduleBackstop(any()) }
    }
}
