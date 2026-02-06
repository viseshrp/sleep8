package com.sleep8.domain.manager

import android.content.Context
import com.sleep8.data.db.entity.MonitoringStartEventEntity
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.MonitoringReliabilityRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.MonitoringReasonBucket
import com.sleep8.domain.model.MonitoringTriggerSource
import com.sleep8.domain.model.Settings
import com.sleep8.domain.scheduler.MonitoringHealthScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.MonitoringRuntimeInspector
import com.sleep8.service.ServiceController
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringReliabilityManagerTest {

    @Test
    fun `trigger starts monitoring when armed and in window`() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>()
        val serviceController = mockk<ServiceController>(relaxed = true)
        val runtime = mockk<MonitoringRuntimeInspector>()
        val healthScheduler = mockk<MonitoringHealthScheduler>(relaxed = true)
        val repository = mockk<MonitoringReliabilityRepository>(relaxed = true)
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.expectedNightWindowStartTs = System.currentTimeMillis() - 1_000L
        prefs.expectedNightWindowStartScheduledAtTs = System.currentTimeMillis() - 2_000L
        val stateHolder = StateHolder(prefs)
        stateHolder.setState(AppState.ARMED_IDLE)

        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "00:00",
            nightEnd = "23:59",
            confirmOffMinutes = 20,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = false
        )
        every { runtime.isMonitoringActive(any()) } returnsMany listOf(false, true, true)
        coEvery { repository.hasBoundaryExecution(any()) } returns true

        val manager = MonitoringReliabilityManager(
            settingsRepository = settingsRepository,
            stateHolder = stateHolder,
            serviceController = serviceController,
            monitoringRuntimeInspector = runtime,
            monitoringHealthScheduler = healthScheduler,
            appPreferences = prefs,
            reliabilityRepository = repository
        )

        manager.onTrigger(mockk<Context>(relaxed = true), MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM)

        verify { serviceController.startNightMonitorService() }
        coVerify {
            repository.recordTrigger(
                expectedBoundaryTs = any(),
                scheduledAtTs = any(),
                observedAtTs = any(),
                armedAtBoundary = true,
                inNightWindowAtBoundary = true,
                gateOpen = true,
                boundaryTriggerExecuted = true,
                monitoringActive = true,
                monitoringActivatedAtTs = any(),
                reasonBucket = MonitoringReasonBucket.NONE,
                triggerSource = MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM
            )
        }
    }

    @Test
    fun `app launch reconcile classifies likely force-stopped when boundary never executed`() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>()
        val serviceController = mockk<ServiceController>()
        every { serviceController.startNightMonitorService() } just runs
        val runtime = mockk<MonitoringRuntimeInspector>()
        val healthScheduler = mockk<MonitoringHealthScheduler>(relaxed = true)
        val repository = mockk<MonitoringReliabilityRepository>(relaxed = true)
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.expectedNightWindowStartTs = System.currentTimeMillis() - 30 * 60 * 1_000L
        prefs.expectedNightWindowStartScheduledAtTs = prefs.expectedNightWindowStartTs - 1_000L
        val stateHolder = StateHolder(prefs)
        stateHolder.setState(AppState.ARMED_IDLE)

        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "00:00",
            nightEnd = "23:59",
            confirmOffMinutes = 20,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = false
        )
        every { runtime.isMonitoringActive(any()) } returns false
        coEvery { repository.hasBoundaryExecution(any()) } returns false
        coEvery { repository.latest() } returns MonitoringStartEventEntity(
            expectedBoundaryTs = prefs.expectedNightWindowStartTs,
            scheduledAtTs = prefs.expectedNightWindowStartScheduledAtTs,
            boundaryObservedAtTs = null,
            armedAtBoundary = true,
            inNightWindowAtBoundary = true,
            gateOpen = true,
            boundaryTriggerExecuted = false,
            monitoringActive = false,
            monitoringActivatedAtTs = null,
            reasonBucket = MonitoringReasonBucket.APP_RESTRICTED_OR_FORCE_STOPPED_SUSPECTED.label,
            triggerSource = MonitoringTriggerSource.APP_LAUNCH_RECONCILE.value,
            createdAtTs = System.currentTimeMillis()
        )

        val manager = MonitoringReliabilityManager(
            settingsRepository = settingsRepository,
            stateHolder = stateHolder,
            serviceController = serviceController,
            monitoringRuntimeInspector = runtime,
            monitoringHealthScheduler = healthScheduler,
            appPreferences = prefs,
            reliabilityRepository = repository
        )

        manager.reconcileOnForeground(mockk<Context>(relaxed = true))

        coVerify {
            repository.recordTrigger(
                expectedBoundaryTs = any(),
                scheduledAtTs = any(),
                observedAtTs = any(),
                armedAtBoundary = true,
                inNightWindowAtBoundary = true,
                gateOpen = true,
                boundaryTriggerExecuted = false,
                monitoringActive = false,
                monitoringActivatedAtTs = null,
                reasonBucket = MonitoringReasonBucket.APP_RESTRICTED_OR_FORCE_STOPPED_SUSPECTED,
                triggerSource = MonitoringTriggerSource.APP_LAUNCH_RECONCILE
            )
        }
        assertEquals(
            MonitoringReasonBucket.APP_RESTRICTED_OR_FORCE_STOPPED_SUSPECTED.label,
            manager.latestReasonLabel()
        )
    }

    @Test
    fun `trigger does not start monitoring when gate closed`() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>()
        val serviceController = mockk<ServiceController>(relaxed = true)
        val runtime = mockk<MonitoringRuntimeInspector>()
        val healthScheduler = mockk<MonitoringHealthScheduler>(relaxed = true)
        val repository = mockk<MonitoringReliabilityRepository>(relaxed = true)
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.expectedNightWindowStartTs = System.currentTimeMillis()
        prefs.expectedNightWindowStartScheduledAtTs = System.currentTimeMillis()
        val stateHolder = StateHolder(prefs)
        stateHolder.setState(AppState.DISARMED)

        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "00:00",
            nightEnd = "23:59",
            confirmOffMinutes = 20,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = false
        )
        every { runtime.isMonitoringActive(any()) } returns false

        val manager = MonitoringReliabilityManager(
            settingsRepository = settingsRepository,
            stateHolder = stateHolder,
            serviceController = serviceController,
            monitoringRuntimeInspector = runtime,
            monitoringHealthScheduler = healthScheduler,
            appPreferences = prefs,
            reliabilityRepository = repository
        )

        manager.onTrigger(mockk<Context>(relaxed = true), MonitoringTriggerSource.PERIODIC_HEALTH_CHECK)

        verify(exactly = 0) { serviceController.startNightMonitorService() }
        coVerify {
            repository.recordTrigger(
                expectedBoundaryTs = any(),
                scheduledAtTs = any(),
                observedAtTs = any(),
                armedAtBoundary = false,
                inNightWindowAtBoundary = true,
                gateOpen = false,
                boundaryTriggerExecuted = false,
                monitoringActive = false,
                monitoringActivatedAtTs = null,
                reasonBucket = MonitoringReasonBucket.NONE,
                triggerSource = MonitoringTriggerSource.PERIODIC_HEALTH_CHECK
            )
        }
    }
}
