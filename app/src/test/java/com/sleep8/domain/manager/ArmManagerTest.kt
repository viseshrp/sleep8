package com.sleep8.domain.manager

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.Settings
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

class ArmManagerTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val serviceController = mockk<ServiceController>(relaxed = true)
    private val windowScheduler = mockk<WindowScheduler>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()

    private val prefs = AppPreferences(InMemorySharedPreferences())
    private val stateHolder = StateHolder(prefs)

    private val armManager = ArmManager(
        sessionRepository = sessionRepository,
        stateHolder = stateHolder,
        serviceController = serviceController,
        windowScheduler = windowScheduler,
        settingsRepository = settingsRepository
    )

    @Test
    fun `arm creates session with correct source`() = runTest {
        val settings = Settings("22:00", "08:00", 10, null, 8, false, true)
        coEvery { settingsRepository.getSettings() } returns settings
        val session = ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns session

        val result = armManager.arm(ArmSource.APP_BUTTON)

        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `arm starts foreground service`() = runTest {
        val settings = Settings("22:00", "08:00", 10, null, 8, false, true)
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
    fun `arm when already armed is idempotent`() = runTest {
        stateHolder.setArmed(true)
        stateHolder.setActiveSession(ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON))
        val settings = Settings("22:00", "08:00", 10, null, 8, false, true)
        coEvery { settingsRepository.getSettings() } returns settings

        armManager.arm(ArmSource.APP_BUTTON)

        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), any()) }
    }

    @Test
    fun `auto-arm arms at night start and disarms at night end`() = runTest {
        val settings = Settings("22:00", "08:00", 10, null, 8, false, true)
        coEvery { settingsRepository.getSettings() } returns settings.copy(autoArmEnabled = true)

        armManager.handleAutoArm()
        coVerify { windowScheduler.scheduleWindowStart(any()) }
        coVerify { windowScheduler.scheduleWindowEnd(any()) }
    }

    @Test
    fun `manual disarm overrides scheduled arming until next event`() = runTest {
        val settings = Settings("22:00", "08:00", 10, null, 8, false, true)
        coEvery { settingsRepository.getSettings() } returns settings
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns ArmSession(5L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON)

        armManager.arm(ArmSource.APP_BUTTON)
        armManager.disarm(ArmSource.APP_BUTTON)
        clearMocks(sessionRepository)
        armManager.onScheduledEvent("start")
        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), ArmSource.SCHEDULED) }
        // manualOverride resets after scheduled event; next event can arm again.
    }

    @Test
    fun `arm uses correct ArmSource for scheduled and manual`() = runTest {
        val settings = Settings("22:00", "08:00", 10, null, 8, false, true)
        coEvery { settingsRepository.getSettings() } returns settings
        coEvery { sessionRepository.createSession(any(), any(), any(), any()) } returns ArmSession(6L, 0L, null, 0L, 0L, ArmSource.SCHEDULED)

        armManager.arm(ArmSource.SCHEDULED)
        coVerify { sessionRepository.createSession(any(), any(), any(), ArmSource.SCHEDULED) }
        armManager.arm(ArmSource.APP_BUTTON)
        coVerify(exactly = 0) { sessionRepository.createSession(any(), any(), any(), ArmSource.APP_BUTTON) }
    }
}
