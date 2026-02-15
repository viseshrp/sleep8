package com.sleep8.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.db.Sleep8Database
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.NightWindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class FullFlowIntegrationTest {

    private lateinit var db: Sleep8Database
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var alarmRepository: AlarmRepository

    private lateinit var stateHolder: StateHolder
    private lateinit var armManager: ArmManager
    private lateinit var stateMachineManager: StateMachineManager
    private lateinit var appPreferences: AppPreferences
    private lateinit var alarmScheduler: AlarmScheduler

    @Before
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, Sleep8Database::class.java).build()
        settingsRepository = SettingsRepository(db.settingsDao())
        sessionRepository = SessionRepository(db.armSessionDao(), db.screenEventDao())
        alarmRepository = AlarmRepository(db.alarmRecordDao())

        appPreferences = AppPreferences(InMemorySharedPreferences())
        stateHolder = StateHolder(appPreferences)
        val serviceController = mockk<ServiceController>(relaxed = true)
        val confirmScheduler = mockk<ConfirmOffScheduler>(relaxed = true)
        val nightWindowScheduler = mockk<NightWindowScheduler>(relaxed = true)
        val monitoringReliabilityManager = mockk<MonitoringReliabilityManager>(relaxed = true)

        alarmScheduler = AlarmScheduler(
            context,
            context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager,
            alarmRepository,
            settingsRepository,
            appPreferences,
            NotificationHelper(context)
        )

        armManager = ArmManager(
            sessionRepository,
            stateHolder,
            serviceController,
            settingsRepository,
            nightWindowScheduler,
            confirmScheduler,
            monitoringReliabilityManager,
            NotificationHelper(context)
        )
        stateMachineManager = StateMachineManager(
            stateHolder,
            settingsRepository,
            sessionRepository,
            alarmRepository,
            confirmScheduler,
            alarmScheduler,
            appPreferences
        )
    }

    @After
    fun teardown() {
    }

    @Test
    fun `complete flow - arm to alarm creation`() = runTest {
        armManager.arm(ArmSource.APP_BUTTON)
        assertEquals(AppState.ARMED_IDLE, stateMachineManager.currentState)

        val screenOffInstant = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
        stateMachineManager.onScreenOff(screenOffInstant)
        assertEquals(AppState.ARMED_PENDING_CONFIRM, stateMachineManager.currentState)

        stateMachineManager.onConfirmationTimerExpired(screenStillOff = true)
        assertEquals(AppState.ARMED_ALARM_SET, stateMachineManager.currentState)

        val session = sessionRepository.getActiveSession()
        assertNotNull(session)
        val alarms = alarmRepository.getAlarmsForSession(session!!.id)
        assertEquals(1, alarms.size)
    }

    @Test
    fun `manual disarm preserves existing alarm and clears pending confirmation`() = runTest {
        armManager.arm(ArmSource.APP_BUTTON)
        val record = alarmScheduler.scheduleSleepAlarm(
            screenOffTs = System.currentTimeMillis() - 60_000L,
            confirmedAt = System.currentTimeMillis()
        )
        stateHolder.setPendingCandidate(123L, 456L)

        armManager.disarm(ArmSource.APP_BUTTON)

        val scheduled = alarmRepository.getScheduledRecords()
        assertEquals(1, scheduled.size)
        assertEquals(record.id, scheduled.first().id)
        assertEquals(-1L, stateHolder.pendingCandidateScreenOffTs.value)
        assertEquals(-1L, stateHolder.pendingConfirmDeadlineTs.value)
    }
}
