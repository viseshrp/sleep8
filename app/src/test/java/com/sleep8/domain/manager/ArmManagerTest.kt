package com.sleep8.domain.manager

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.Settings
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.WindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ArmManagerTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val serviceController = mockk<ServiceController>(relaxed = true)
    private val windowScheduler = mockk<WindowScheduler>(relaxed = true)
    private val nightWindowScheduler = mockk<com.sleep8.domain.scheduler.NightWindowScheduler>(relaxed = true)
    private val confirmOffScheduler = mockk<ConfirmOffScheduler>(relaxed = true)
    private val monitoringReliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()

    private val prefs = AppPreferences(InMemorySharedPreferences())
    private val stateHolder = StateHolder(prefs)

    private val armManager = ArmManager(
        sessionRepository = sessionRepository,
        stateHolder = stateHolder,
        serviceController = serviceController,
        windowScheduler = windowScheduler,
        settingsRepository = settingsRepository,
        nightWindowScheduler = nightWindowScheduler,
        confirmOffScheduler = confirmOffScheduler,
        monitoringReliabilityManager = monitoringReliabilityManager
    )

    @Test
    fun `arm creates session with correct source`() = runTest {
        val settings = Settings(
            nightStart = "00:00",
            nightEnd = "23:59",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { settingsRepository.getSettings() } returns settings
        val session = ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns session

        val result = armManager.arm(ArmSource.APP_BUTTON)

        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `arm starts foreground service`() = runTest {
        val now = LocalTime.now()
        val start = now.minusMinutes(1)
        val end = now.plusMinutes(1)
        val settings = Settings(
            nightStart = start.toString().substring(0, 5),
            nightEnd = end.toString().substring(0, 5),
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { settingsRepository.getSettings() } returns settings
        val session = ArmSession(1L, 0L, null, 0L, 0L, ArmSource.QUICK_TILE)
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns session

        armManager.arm(ArmSource.QUICK_TILE)

        coVerify { serviceController.startNightMonitorService() }
    }

    @Test
    fun `disarm ends session`() = runTest {
        val session = ArmSession(3L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)

        armManager.disarm()

        coVerify { sessionRepository.endSession(3L, any()) }
    }

    @Test
    fun `disarm stops foreground service`() = runTest {
        val session = ArmSession(2L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)

        armManager.disarm()

        coVerify { serviceController.stopNightMonitorService() }
    }

    @Test
    fun `disarm clears pending confirmation and cancels timer`() = runTest {
        val session = ArmSession(9L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)
        stateHolder.setPendingCandidate(123L, 456L)

        armManager.disarm()

        assertTrue(stateHolder.pendingCandidateScreenOffTs.value < 0)
        assertTrue(stateHolder.pendingConfirmDeadlineTs.value < 0)
        coVerify { confirmOffScheduler.cancelConfirmation() }
    }

    @Test
    fun `auto-disarm clears pending confirmation and cancels timer`() = runTest {
        val session = ArmSession(10L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)
        stateHolder.setPendingCandidate(321L, 654L)

        armManager.disarm(ArmSource.SCHEDULED)

        assertTrue(stateHolder.pendingCandidateScreenOffTs.value < 0)
        assertTrue(stateHolder.pendingConfirmDeadlineTs.value < 0)
        coVerify { confirmOffScheduler.cancelConfirmation() }
    }

    @Test
    fun `arm when already armed is idempotent`() = runTest {
        stateHolder.setArmed(true)
        stateHolder.setActiveSession(ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON))
        val settings = Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { settingsRepository.getSettings() } returns settings

        armManager.arm(ArmSource.APP_BUTTON)

        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), any()) }
    }

    @Test
    fun `auto-arm arms at night start and disarms at night end`() = runTest {
        val settings = Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { settingsRepository.getSettings() } returns settings.copy(autoArmEnabled = true)

        armManager.handleAutoArm()
        coVerify { windowScheduler.scheduleWindowStart(any()) }
        coVerify { windowScheduler.scheduleWindowEnd(any()) }
    }

    @Test
    fun `manual disarm does not block auto-arm start`() = runTest {
        val settings = Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { settingsRepository.getSettings() } returns settings.copy(autoArmEnabled = true)
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns ArmSession(5L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)

        armManager.arm(ArmSource.APP_BUTTON)
        armManager.disarm(ArmSource.APP_BUTTON)
        clearMocks(sessionRepository)
        armManager.onScheduledEvent("start")
        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.SCHEDULED) }
    }

    @Test
    fun `manual arm does not block auto-disarm end`() = runTest {
        val settings = Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { settingsRepository.getSettings() } returns settings.copy(autoArmEnabled = true)
        val session = ArmSession(7L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)

        armManager.onScheduledEvent("end")
        coVerify { sessionRepository.endSession(7L, any()) }
    }

    @Test
    fun `arm uses correct ArmSource for scheduled and manual`() = runTest {
        val settings = Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true
        )
        coEvery { settingsRepository.getSettings() } returns settings
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns ArmSession(6L, 0L, null, 0L, 0L, ArmSource.SCHEDULED)

        armManager.arm(ArmSource.SCHEDULED)
        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.SCHEDULED) }
        armManager.arm(ArmSource.APP_BUTTON)
        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
    }

    @Test
    fun `refresh night window while disarmed stops monitor and cancels night schedulers`() = runTest {
        stateHolder.setArmed(false)

        armManager.refreshNightWindowBoundariesIfArmed()

        coVerify { serviceController.stopNightMonitorService() }
        coVerify { nightWindowScheduler.cancelWindowStart() }
        coVerify { nightWindowScheduler.cancelWindowEnd() }
    }

    @Test
    fun `disable auto arm cancels scheduled auto-arm boundaries`() = runTest {
        armManager.updateAutoArmEnabled(false)

        coVerify { windowScheduler.cancelWindowStart() }
        coVerify { windowScheduler.cancelWindowEnd() }
    }

    @Test
    fun `handle auto arm arms immediately when current time is inside auto window`() = runTest {
        val now = LocalTime.now()
        val settings = Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = true,
            autoArmStart = now.minusMinutes(1).toString().substring(0, 5),
            autoArmEnd = now.plusMinutes(1).toString().substring(0, 5)
        )
        coEvery { settingsRepository.getSettings() } returns settings
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns
            ArmSession(50L, 0L, null, 0L, 0L, ArmSource.SCHEDULED)

        armManager.handleAutoArm()

        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.SCHEDULED) }
    }

    @Test
    fun `handle auto arm exits early when feature disabled`() = runTest {
        val settings = Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = false
        )
        coEvery { settingsRepository.getSettings() } returns settings

        armManager.handleAutoArm()

        coVerify(exactly = 0) { windowScheduler.scheduleWindowStart(any()) }
        coVerify(exactly = 0) { windowScheduler.scheduleWindowEnd(any()) }
    }

    @Test
    fun `unknown scheduled event type does nothing`() {
        armManager.onScheduledEvent("noop")

        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), any()) }
        coVerify(exactly = 0) { sessionRepository.endSession(any(), any()) }
    }
}
