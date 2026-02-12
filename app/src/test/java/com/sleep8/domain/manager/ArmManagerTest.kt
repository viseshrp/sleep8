package com.sleep8.domain.manager

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.NightWindow
import com.sleep8.domain.model.Settings
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.NightWindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.util.TimeUtils
import io.mockk.coVerify
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ArmManagerTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val serviceController = mockk<ServiceController>(relaxed = true)
    private val nightWindowScheduler = mockk<NightWindowScheduler>(relaxed = true)
    private val confirmOffScheduler = mockk<ConfirmOffScheduler>(relaxed = true)
    private val monitoringReliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()

    private val prefs = AppPreferences(InMemorySharedPreferences())
    private val stateHolder = StateHolder(prefs)

    private val armManager = ArmManager(
        sessionRepository = sessionRepository,
        stateHolder = stateHolder,
        serviceController = serviceController,
        settingsRepository = settingsRepository,
        nightWindowScheduler = nightWindowScheduler,
        confirmOffScheduler = confirmOffScheduler,
        monitoringReliabilityManager = monitoringReliabilityManager,
        notificationHelper = notificationHelper
    )

    private val settings = Settings(
        nightStart = "00:00",
        nightEnd = "23:59",
        confirmOffMinutes = 10,
        alarmDurationMinutes = 480,
        overlayEnabled = false,
        armedDefault = false
    )

    @AfterEach
    fun tearDown() {
        unmockkObject(TimeUtils)
    }

    @Test
    fun `arm creates session with manual source`() = runTest {
        coEvery { settingsRepository.getSettings() } returns settings
        val session = ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns session
        stateHolder.setLastScreenOffTs(999L)

        val result = armManager.arm(ArmSource.APP_BUTTON)

        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
        assertTrue(result.isSuccess)
        assertTrue(stateHolder.lastScreenOffTs.value < 0)
    }

    @Test
    fun `disarm clears pending confirmation and keeps existing alarms untouched`() = runTest {
        val session = ArmSession(9L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)
        stateHolder.setPendingCandidate(123L, 456L)
        stateHolder.setLastScreenOffTs(789L)

        armManager.disarm()

        assertTrue(stateHolder.pendingCandidateScreenOffTs.value < 0)
        assertTrue(stateHolder.pendingConfirmDeadlineTs.value < 0)
        assertTrue(stateHolder.lastScreenOffTs.value < 0)
        verify { confirmOffScheduler.cancelConfirmation() }
        verify { notificationHelper.clearAllPendingNotifications() }
    }

    @Test
    fun `arm when already armed is idempotent`() = runTest {
        stateHolder.setArmed(true)
        stateHolder.setActiveSession(ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON))
        coEvery { settingsRepository.getSettings() } returns settings

        armManager.arm(ArmSource.APP_BUTTON)

        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), any()) }
    }

    @Test
    fun `arm when already armed without active session returns fallback session`() = runTest {
        stateHolder.setArmed(true)
        stateHolder.setActiveSession(null)
        coEvery { settingsRepository.getSettings() } returns settings

        val result = armManager.arm(ArmSource.APP_BUTTON)

        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrNull()?.id)
        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), any()) }
    }

    @Test
    fun `refresh night window while disarmed stops monitor and cancels night schedulers`() = runTest {
        stateHolder.setArmed(false)

        armManager.refreshNightWindowBoundariesIfArmed()

        verify { serviceController.stopNightMonitorService() }
        verify { nightWindowScheduler.cancelWindowStart() }
        verify { nightWindowScheduler.cancelWindowEnd() }
        verify { nightWindowScheduler.cancelWindowStartBackstops() }
    }

    @Test
    fun `manual arm refreshes night-window boundaries only`() = runTest {
        val windowSettings = settings.copy(nightStart = "22:00", nightEnd = "08:00")
        coEvery { settingsRepository.getSettings() } returns windowSettings
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns
            ArmSession(50L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)

        armManager.arm(ArmSource.APP_BUTTON)

        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
        verify { nightWindowScheduler.scheduleWindowStart(any()) }
        verify { nightWindowScheduler.scheduleWindowEnd(any()) }
    }

    @Test
    fun `refresh while armed and outside window schedules next boundaries and records expected start`() = runTest {
        stateHolder.setArmed(true)
        val windowSettings = settings.copy(nightStart = "22:00", nightEnd = "08:00")
        coEvery { settingsRepository.getSettings() } returns windowSettings
        mockkObject(TimeUtils)
        every { TimeUtils.parseLocalTime("22:00") } returns LocalTime.of(22, 0)
        every { TimeUtils.parseLocalTime("08:00") } returns LocalTime.of(8, 0)
        every { TimeUtils.isInWindow(any(), any(), any()) } returns false
        every { TimeUtils.calculateNextWindow(any<LocalDateTime>(), any(), any()) } returns
            NightWindow(startTs = 10_000L, endTs = 20_000L)

        armManager.refreshNightWindowBoundariesIfArmed()

        verify { serviceController.stopNightMonitorService() }
        verify { nightWindowScheduler.scheduleWindowStart(10_000L) }
        verify { nightWindowScheduler.scheduleWindowEnd(20_000L) }
        verify { nightWindowScheduler.scheduleWindowStartBackstops(10_000L) }
        coVerify(timeout = 1_000) { monitoringReliabilityManager.recordNightWindowStartSchedule(10_000L) }
    }

    @Test
    fun `observe armed state emits false then true`() = runTest {
        val initial = armManager.observeArmedState().first()
        stateHolder.setArmed(true)
        val updated = armManager.observeArmedState().first()

        assertEquals(false, initial)
        assertEquals(true, updated)
    }
}
