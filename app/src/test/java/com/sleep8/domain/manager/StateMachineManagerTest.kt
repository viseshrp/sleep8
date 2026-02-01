package com.sleep8.domain.manager

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.Settings
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.OsAlarmCreator
import com.sleep8.domain.state.StateHolder
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.TimeZone

class StateMachineManagerTest {

    @BeforeEach
    fun setupTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private val settingsRepository = mockk<SettingsRepository>()
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
    private val confirmScheduler = mockk<ConfirmOffScheduler>(relaxed = true)
    private val osAlarmCreator = mockk<OsAlarmCreator>(relaxed = true)

    private val prefs = AppPreferences(InMemorySharedPreferences())
    private val stateHolder = StateHolder(prefs)

    private val manager = StateMachineManager(
        stateHolder = stateHolder,
        settingsRepository = settingsRepository,
        sessionRepository = sessionRepository,
        alarmRepository = alarmRepository,
        confirmOffScheduler = confirmScheduler,
        osAlarmCreator = osAlarmCreator
    )

    private suspend fun setupSession() {
        stateHolder.setActiveSession(ArmSession(1L, 0L, null, 0L, 0L, ArmSource.APP_BUTTON))
        stateHolder.setState(AppState.ARMED_IDLE)
        coEvery { settingsRepository.getSettings() } returns Settings("22:00", "08:00", 10, null, false, true)
    }

    @Test
    fun `disarmed state - arm transitions to armed idle`() {
        stateHolder.setState(AppState.DISARMED)
        stateHolder.setArmed(true)
        assertEquals(AppState.ARMED_IDLE, manager.currentState)
    }

    @Test
    fun `armed idle - screen off in window transitions to pending confirm`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T23:00:00Z"))
        assertEquals(AppState.ARMED_PENDING_CONFIRM, manager.currentState)
    }

    @Test
    fun `armed idle - screen off outside window stays in armed idle`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T12:00:00Z"))
        assertEquals(AppState.ARMED_IDLE, manager.currentState)
    }

    @Test
    fun `pending confirm - screen on transitions to armed idle`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T23:00:00Z"))
        manager.onScreenOn()
        assertEquals(AppState.ARMED_IDLE, manager.currentState)
    }

    @Test
    fun `pending confirm - timer expired transitions to alarm set`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T23:00:00Z"))
        manager.onConfirmationTimerExpired(screenStillOff = true)
        assertEquals(AppState.ARMED_ALARM_SET, manager.currentState)
    }

    @Test
    fun `pending confirm - timer expired but screen on transitions to armed idle`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T23:00:00Z"))
        manager.onConfirmationTimerExpired(screenStillOff = false)
        assertEquals(AppState.ARMED_IDLE, manager.currentState)
    }

    @Test
    fun `pending confirm - new screen off updates candidate and restarts timer`() = runTest {
        setupSession()
        val firstOffTime = Instant.parse("2024-01-15T23:00:00Z")
        val secondOffTime = Instant.parse("2024-01-15T23:02:00Z")

        manager.onScreenOff(firstOffTime)
        manager.onScreenOff(secondOffTime)

        assertEquals(secondOffTime, manager.pendingCandidateTime)
        coVerify(exactly = 2) { confirmScheduler.scheduleConfirmation(any(), any()) }
    }

    @Test
    fun `alarm set - new screen off transitions back to pending confirm`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T23:00:00Z"))
        manager.onConfirmationTimerExpired(screenStillOff = true)
        manager.onScreenOff(Instant.parse("2024-01-16T00:00:00Z"))
        assertEquals(AppState.ARMED_PENDING_CONFIRM, manager.currentState)
    }

    @Test
    fun `any armed state - disarm transitions to disarmed`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T23:00:00Z"))
        manager.disarm()
        assertEquals(AppState.DISARMED, manager.currentState)
    }

    @Test
    fun `disarm cancels confirmation timer`() = runTest {
        setupSession()
        manager.onScreenOff(Instant.parse("2024-01-15T23:00:00Z"))
        manager.disarm()
        coVerify { confirmScheduler.cancelConfirmation() }
    }
}
