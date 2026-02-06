package com.sleep8.domain.manager

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.Settings
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.NightWindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArmManagerTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val serviceController = mockk<ServiceController>(relaxed = true)
    private val nightWindowScheduler = mockk<NightWindowScheduler>(relaxed = true)
    private val confirmOffScheduler = mockk<ConfirmOffScheduler>(relaxed = true)
    private val monitoringReliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)
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
        monitoringReliabilityManager = monitoringReliabilityManager
    )

    private val settings = Settings(
        nightStart = "00:00",
        nightEnd = "23:59",
        confirmOffMinutes = 10,
        alarmDurationMinutes = 480,
        overlayEnabled = false,
        armedDefault = false
    )

    @Test
    fun `arm creates session with manual source`() = runTest {
        coEvery { settingsRepository.getSettings() } returns settings
        val session = ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns session

        val result = armManager.arm(ArmSource.APP_BUTTON)

        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `disarm clears pending confirmation and keeps existing alarms untouched`() = runTest {
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
    fun `arm when already armed is idempotent`() = runTest {
        stateHolder.setArmed(true)
        stateHolder.setActiveSession(ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON))
        coEvery { settingsRepository.getSettings() } returns settings

        armManager.arm(ArmSource.APP_BUTTON)

        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), any()) }
    }

    @Test
    fun `refresh night window while disarmed stops monitor and cancels night schedulers`() = runTest {
        stateHolder.setArmed(false)

        armManager.refreshNightWindowBoundariesIfArmed()

        coVerify { serviceController.stopNightMonitorService() }
        coVerify { nightWindowScheduler.cancelWindowStart() }
        coVerify { nightWindowScheduler.cancelWindowEnd() }
        coVerify { nightWindowScheduler.cancelWindowStartBackstops() }
    }

    @Test
    fun `manual arm refreshes night-window boundaries only`() = runTest {
        val windowSettings = settings.copy(nightStart = "22:00", nightEnd = "08:00")
        coEvery { settingsRepository.getSettings() } returns windowSettings
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns
            ArmSession(50L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)

        armManager.arm(ArmSource.APP_BUTTON)

        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
        coVerify { nightWindowScheduler.scheduleWindowStart(any()) }
        coVerify { nightWindowScheduler.scheduleWindowEnd(any()) }
    }
}
