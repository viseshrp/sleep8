package com.sleep8.service.receiver

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.MonitoringTriggerSource
import com.sleep8.domain.model.Settings
import com.sleep8.domain.state.StateHolder
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.service.ServiceController
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class NightWindowReceiversTest {

    @Test
    fun `night window start starts service and resumes confirmation`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val now = LocalDateTime.of(2026, 2, 4, 22, 30)
        advanceClockTo(now)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIsInteractive(false)
        val expectedScreenStillOff = !powerManager.isInteractive

        val repo = mockk<SettingsRepository>()
        coEvery { repo.getSettings() } returns Settings(
            nightStart = "00:00",
            nightEnd = "23:59",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false)

        val stateHolder = StateHolder(AppPreferences(InMemorySharedPreferences()))
        stateHolder.setState(AppState.ARMED_IDLE)

        val reliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)
        val manager = mockk<StateMachineManager>(relaxed = true)

        val receiver = NightWindowStartReceiver().apply {
            this.stateHolder = stateHolder
            this.settingsRepository = repo
            this.monitoringReliabilityManager = reliabilityManager
            this.stateMachineManager = manager
            this.dispatcher = Dispatchers.Default
        }

        runBlocking { receiver.handleNightWindowStart(context) }

        coVerify(timeout = 1000) {
            reliabilityManager.onTrigger(context, MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM)
        }
        coVerify(timeout = 1000) { manager.resumePendingConfirmationIfEligible(expectedScreenStillOff) }
    }

    @Test
    fun `night window end stops service and clears pending state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stateHolder = StateHolder(AppPreferences(InMemorySharedPreferences()))
        stateHolder.setState(AppState.ARMED_IDLE)

        val service = mockk<ServiceController>(relaxed = true)
        val manager = mockk<StateMachineManager>(relaxed = true)
        val reliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)

        val receiver = NightWindowEndReceiver().apply {
            this.stateHolder = stateHolder
            this.serviceController = service
            this.stateMachineManager = manager
            this.monitoringReliabilityManager = reliabilityManager
            this.dispatcher = Dispatchers.Default
        }

        runBlocking { receiver.handleNightWindowEnd() }

        verify(timeout = 1000) { service.stopNightMonitorService() }
        verify(timeout = 1000) { reliabilityManager.onNightWindowEnded() }
        coVerify(timeout = 1000) { manager.onNightWindowEnd() }
    }

    @Test
    fun `night window start does not trigger monitoring when disarmed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = mockk<SettingsRepository>()
        coEvery { repo.getSettings() } returns Settings(
            nightStart = "00:00",
            nightEnd = "23:59",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false)

        val stateHolder = StateHolder(AppPreferences(InMemorySharedPreferences()))
        stateHolder.setState(AppState.DISARMED)
        val reliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)
        val manager = mockk<StateMachineManager>(relaxed = true)

        val receiver = NightWindowStartReceiver().apply {
            this.stateHolder = stateHolder
            this.settingsRepository = repo
            this.monitoringReliabilityManager = reliabilityManager
            this.stateMachineManager = manager
            this.dispatcher = Dispatchers.Default
        }

        runBlocking { receiver.handleNightWindowStart(context) }

        coVerify(exactly = 0) { reliabilityManager.onTrigger(any(), any()) }
        coVerify(exactly = 0) { manager.resumePendingConfirmationIfEligible(any()) }
    }

    private fun advanceClockTo(target: LocalDateTime) {
        val targetMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val current = ShadowSystemClock.currentTimeMillis()
        val delta = targetMillis - current
        val advanceBy = if (delta <= 0) delta + TimeUnit.DAYS.toMillis(1) else delta
        ShadowSystemClock.advanceBy(advanceBy, TimeUnit.MILLISECONDS)
    }
}
