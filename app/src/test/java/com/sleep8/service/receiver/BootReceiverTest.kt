package com.sleep8.service.receiver

import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.Settings
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.util.TimeZone

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class BootReceiverTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()
    private val serviceController = mockk<ServiceController>(relaxed = true)
    private val confirmOffScheduler = mockk<ConfirmOffScheduler>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val stateMachineManager = mockk<StateMachineManager>(relaxed = true)
    private val armManager = mockk<ArmManager>(relaxed = true)
    private val monitoringReliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)

    private val prefs = AppPreferences(InMemorySharedPreferences())
    private val stateHolder = StateHolder(prefs)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun `no active session on boot stays disarmed`() {
        coEvery { sessionRepository.getActiveSession() } returns null

        val receiver = receiver()

        runBlocking { receiver.handleBoot(context) }

        org.junit.Assert.assertEquals(AppState.DISARMED, stateHolder.state.value)
        verify(timeout = 1000) { serviceController.stopNightMonitorService() }
    }

    @Test
    fun `expired session on boot stays armed and is not auto-disarmed`() {
        val now = System.currentTimeMillis()
        val session = ArmSession(1L, now - 5_000L, null, now - 120_000L, now - 60_000L, ArmSource.APP_BUTTON)
        coEvery { sessionRepository.getActiveSession() } returns session
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )

        val receiver = receiver()

        runBlocking { receiver.handleBoot(context) }

        coVerify(exactly = 0) { sessionRepository.endSession(any(), any()) }
        org.junit.Assert.assertEquals(AppState.ARMED_IDLE, stateHolder.state.value)
    }

    @Test
    fun `pending confirmation after reboot schedules alarm immediately when overdue`() {
        val now = LocalDateTime.now()
        val nightStart = now.minusHours(1).toLocalTime()
        val nightEnd = now.plusHours(1).toLocalTime()

        val settings = Settings(
            nightStart = formatTime(nightStart),
            nightEnd = formatTime(nightEnd),
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        coEvery { settingsRepository.getSettings() } returns settings

        val session = ArmSession(
            1L,
            0L,
            null,
            System.currentTimeMillis() - 60_000L,
            System.currentTimeMillis() + 60_000L,
            ArmSource.APP_BUTTON
        )
        coEvery { sessionRepository.getActiveSession() } returns session
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)
        stateHolder.setState(AppState.ARMED_IDLE)

        val pendingScreenOff = System.currentTimeMillis() - 20 * 60_000L
        val pendingDeadline = System.currentTimeMillis() - 5 * 60_000L
        stateHolder.setPendingCandidate(pendingScreenOff, pendingDeadline)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIsInteractive(false)

        val receiver = receiver()

        runBlocking { receiver.handleBoot(context) }
        coVerify(timeout = 1000) { alarmScheduler.scheduleSleepAlarm(pendingScreenOff, any()) }
        org.junit.Assert.assertEquals(AppState.ARMED_ALARM_SET, stateHolder.state.value)
    }

    @Test
    fun `reboot reschedules overdue scheduled alarm`() {
        val settings = Settings(
            nightStart = "00:00",
            nightEnd = "23:59",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        coEvery { settingsRepository.getSettings() } returns settings

        val session = ArmSession(1L, 0L, null, 0L, System.currentTimeMillis() + 120_000L, ArmSource.APP_BUTTON)
        coEvery { sessionRepository.getActiveSession() } returns session
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)
        stateHolder.setState(AppState.ARMED_IDLE)

        val record = AlarmRecord(
            id = 10L,
            sessionId = 1L,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = System.currentTimeMillis() - 60_000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 111L,
            requestCode = 111,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        coEvery { alarmScheduler.reconcileScheduledAfterBoot() } returns record

        val receiver = receiver()

        runBlocking { receiver.handleBoot(context) }
        verify(timeout = 1000) { alarmScheduler.rescheduleExisting(record, any()) }
    }

    private fun receiver() = BootReceiver().apply {
        this.sessionRepository = this@BootReceiverTest.sessionRepository
        this.settingsRepository = this@BootReceiverTest.settingsRepository
        this.stateHolder = this@BootReceiverTest.stateHolder
        this.serviceController = this@BootReceiverTest.serviceController
        this.confirmOffScheduler = this@BootReceiverTest.confirmOffScheduler
        this.alarmScheduler = this@BootReceiverTest.alarmScheduler
        this.stateMachineManager = this@BootReceiverTest.stateMachineManager
        this.armManager = this@BootReceiverTest.armManager
        this.monitoringReliabilityManager = this@BootReceiverTest.monitoringReliabilityManager
    }

    private fun formatTime(time: java.time.LocalTime): String {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
}
