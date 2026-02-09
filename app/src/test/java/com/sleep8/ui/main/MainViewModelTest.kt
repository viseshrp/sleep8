package com.sleep8.ui.main

import android.content.Context
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.Settings
import com.sleep8.domain.state.StateHolder
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.util.PermissionUtils
import com.sleep8.util.TimeUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainViewModelTest {

    private val armManager = mockk<ArmManager>(relaxed = true)
    private val reliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)
    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val stateHolder = StateHolder(AppPreferences(InMemorySharedPreferences()))
    private val dispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkObject(PermissionUtils)
        every { PermissionUtils.canPostNotifications(any()) } returns true
        every { PermissionUtils.isServiceRunning(any(), any()) } returns false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkObject(PermissionUtils)
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `init publishes state with latest alarm and pending countdown`() = runTest {
        val now = LocalDateTime.now()
        val nightStart = now.minusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"))
        val nightEnd = now.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"))
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = nightStart,
            nightEnd = nightEnd,
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        coEvery { alarmRepository.getLatestScheduledRecord() } returns sampleAlarm(triggerAt = System.currentTimeMillis() + 10_000)
        coEvery { reliabilityManager.latestReasonLabel() } returns "process not started"

        stateHolder.setState(AppState.ARMED_PENDING_CONFIRM)
        stateHolder.setLastScreenOffTs(System.currentTimeMillis())
        stateHolder.setPendingCandidate(System.currentTimeMillis() - 5_000, System.currentTimeMillis() + 40_000)

        val viewModel = MainViewModel(
            armManager = armManager,
            monitoringReliabilityManager = reliabilityManager,
            stateHolder = stateHolder,
            alarmRepository = alarmRepository,
            settingsRepository = settingsRepository,
            context = context
        )
        runCurrent()

        assertTrue(viewModel.startupReady.value)
        assertEquals("Confirming screen off", viewModel.uiState.value.statusText)
        assertTrue(viewModel.uiState.value.showPending)
        assertTrue(viewModel.uiState.value.pendingCountdownText.isNotBlank())
        assertTrue(viewModel.uiState.value.latestAlarmText.startsWith("Alarm scheduled for "))
        assertTrue(viewModel.uiState.value.monitoringHealthText.startsWith("Degraded"))

        clearViewModel(viewModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleArmed calls disarm when already armed`() = runTest {
        coEvery { settingsRepository.getSettings() } returns defaultSettings()
        coEvery { alarmRepository.getLatestScheduledRecord() } returns null
        coEvery { reliabilityManager.latestReasonLabel() } returns ""
        every { armManager.isArmed() } returns true

        val viewModel = MainViewModel(
            armManager = armManager,
            monitoringReliabilityManager = reliabilityManager,
            stateHolder = stateHolder,
            alarmRepository = alarmRepository,
            settingsRepository = settingsRepository,
            context = context
        )
        runCurrent()

        viewModel.toggleArmed()
        runCurrent()

        coVerify(exactly = 1) { armManager.disarm() }
        clearViewModel(viewModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleArmed calls arm when currently disarmed`() = runTest {
        coEvery { settingsRepository.getSettings() } returns defaultSettings()
        coEvery { alarmRepository.getLatestScheduledRecord() } returns null
        coEvery { reliabilityManager.latestReasonLabel() } returns ""
        every { armManager.isArmed() } returns false

        val viewModel = MainViewModel(
            armManager = armManager,
            monitoringReliabilityManager = reliabilityManager,
            stateHolder = stateHolder,
            alarmRepository = alarmRepository,
            settingsRepository = settingsRepository,
            context = context
        )
        runCurrent()

        viewModel.toggleArmed()
        runCurrent()

        coVerify(exactly = 1) { armManager.arm(ArmSource.APP_BUTTON) }
        clearViewModel(viewModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `refreshOnResume reconciles foreground and refreshes latest alarm`() = runTest {
        coEvery { settingsRepository.getSettings() } returns defaultSettings()
        coEvery { alarmRepository.getLatestScheduledRecord() } returns null
        coEvery { reliabilityManager.latestReasonLabel() } returns ""

        val viewModel = MainViewModel(
            armManager = armManager,
            monitoringReliabilityManager = reliabilityManager,
            stateHolder = stateHolder,
            alarmRepository = alarmRepository,
            settingsRepository = settingsRepository,
            context = context
        )
        runCurrent()

        viewModel.refreshOnResume()
        runCurrent()

        coVerify(exactly = 2) { reliabilityManager.reconcileOnForeground(context) }
        coVerify(atLeast = 2) { alarmRepository.getLatestScheduledRecord() }
        clearViewModel(viewModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `last screen off text is shown even when timestamp is previous day`() = runTest {
        coEvery { settingsRepository.getSettings() } returns defaultSettings()
        coEvery { alarmRepository.getLatestScheduledRecord() } returns null
        coEvery { reliabilityManager.latestReasonLabel() } returns ""
        val yesterdayTs = LocalDateTime.of(2026, 1, 2, 12, 30)
            .minusDays(1)
            .withHour(22)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        stateHolder.setState(AppState.ARMED_IDLE)
        stateHolder.setLastScreenOffTs(yesterdayTs)

        val viewModel = MainViewModel(
            armManager = armManager,
            monitoringReliabilityManager = reliabilityManager,
            stateHolder = stateHolder,
            alarmRepository = alarmRepository,
            settingsRepository = settingsRepository,
            context = context
        )
        runCurrent()

        val expected = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(yesterdayTs))
        assertEquals(expected, viewModel.uiState.value.lastScreenOffText)
        clearViewModel(viewModel)
    }

    private fun defaultSettings() = Settings(
        nightStart = "22:00",
        nightEnd = "08:00",
        confirmOffMinutes = 10,
        alarmDurationMinutes = 480,
        overlayEnabled = false,
        armedDefault = false
    )

    private fun sampleAlarm(triggerAt: Long): AlarmRecord {
        return AlarmRecord(
            id = 1L,
            sessionId = 1L,
            screenOffTs = triggerAt - 600_000L,
            confirmedAt = triggerAt - 540_000L,
            scheduledAt = triggerAt - 530_000L,
            triggerAt = triggerAt,
            durationUsedMinutes = 480,
            alarmInstanceId = 1001L,
            requestCode = 101,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
    }

    private fun clearViewModel(viewModel: MainViewModel) {
        val method = androidx.lifecycle.ViewModel::class.java.declaredMethods.first {
            it.parameterCount == 0 && it.name.contains("clear")
        }
        method.isAccessible = true
        method.invoke(viewModel)
    }
}
