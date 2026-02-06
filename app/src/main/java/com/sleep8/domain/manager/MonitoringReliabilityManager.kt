package com.sleep8.domain.manager

import android.content.Context
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.MonitoringReliabilityRepository
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.MonitoringReasonBucket
import com.sleep8.domain.model.MonitoringTriggerSource
import com.sleep8.domain.scheduler.MonitoringHealthScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.MonitoringRuntimeInspector
import com.sleep8.service.ServiceController
import com.sleep8.util.TimeUtils
import kotlinx.coroutines.delay
import java.time.LocalDateTime

class MonitoringReliabilityManager(
    private val settingsRepository: SettingsRepository,
    private val stateHolder: StateHolder,
    private val serviceController: ServiceController,
    private val monitoringRuntimeInspector: MonitoringRuntimeInspector,
    private val monitoringHealthScheduler: MonitoringHealthScheduler,
    private val appPreferences: AppPreferences,
    private val reliabilityRepository: MonitoringReliabilityRepository
) {

    suspend fun recordNightWindowStartSchedule(expectedBoundaryTs: Long) {
        val scheduledAt = System.currentTimeMillis()
        appPreferences.expectedNightWindowStartTs = expectedBoundaryTs
        appPreferences.expectedNightWindowStartScheduledAtTs = scheduledAt
        reliabilityRepository.recordBoundaryScheduled(expectedBoundaryTs, scheduledAt)
    }

    suspend fun onTrigger(context: Context, triggerSource: MonitoringTriggerSource) {
        val now = System.currentTimeMillis()
        val expectedBoundaryTs = appPreferences.expectedNightWindowStartTs.takeIf { it > 0 } ?: now
        val scheduledAtTs = appPreferences.expectedNightWindowStartScheduledAtTs.takeIf { it > 0 } ?: now
        val settings = settingsRepository.getSettings()
        val start = TimeUtils.parseLocalTime(settings.nightStart)
        val end = TimeUtils.parseLocalTime(settings.nightEnd)
        val inNightWindow = TimeUtils.isInWindow(LocalDateTime.now().toLocalTime(), start, end)
        val armed = stateHolder.state.value != AppState.DISARMED
        val gateOpen = armed && inNightWindow
        val boundaryExecuted = triggerSource == MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM

        var monitoringActive = monitoringRuntimeInspector.isMonitoringActive(context)
        var monitoringActivatedAt: Long? = if (monitoringActive) now else null
        var reasonBucket = MonitoringReasonBucket.NONE

        if (gateOpen && !monitoringActive) {
            reasonBucket = attemptStartMonitoring(context, triggerSource, expectedBoundaryTs)
            monitoringActive = monitoringRuntimeInspector.isMonitoringActive(context)
            monitoringActivatedAt = if (monitoringActive) System.currentTimeMillis() else null
            if (!monitoringActive && reasonBucket == MonitoringReasonBucket.NONE) {
                reasonBucket = MonitoringReasonBucket.UNKNOWN
            }
        }

        reliabilityRepository.recordTrigger(
            expectedBoundaryTs = expectedBoundaryTs,
            scheduledAtTs = scheduledAtTs,
            observedAtTs = now,
            armedAtBoundary = armed,
            inNightWindowAtBoundary = inNightWindow,
            gateOpen = gateOpen,
            boundaryTriggerExecuted = boundaryExecuted,
            monitoringActive = monitoringActive,
            monitoringActivatedAtTs = monitoringActivatedAt,
            reasonBucket = reasonBucket,
            triggerSource = triggerSource
        )

        if (gateOpen) {
            scheduleNextHealthCheck(now)
        } else {
            monitoringHealthScheduler.cancel()
        }
    }

    suspend fun reconcileOnForeground(context: Context) {
        onTrigger(context, MonitoringTriggerSource.APP_LAUNCH_RECONCILE)
    }

    fun onNightWindowEnded() {
        monitoringHealthScheduler.cancel()
    }

    suspend fun latestReasonLabel(): String {
        return reliabilityRepository.latest()?.reasonBucket.orEmpty()
    }

    private suspend fun attemptStartMonitoring(
        context: Context,
        triggerSource: MonitoringTriggerSource,
        expectedBoundaryTs: Long
    ): MonitoringReasonBucket {
        return try {
            serviceController.startNightMonitorService()
            delay(SERVICE_START_VERIFY_DELAY_MS)
            if (monitoringRuntimeInspector.isMonitoringActive(context)) {
                MonitoringReasonBucket.NONE
            } else {
                classifyMissedStart(triggerSource, expectedBoundaryTs)
            }
        } catch (_: SecurityException) {
            MonitoringReasonBucket.START_ATTEMPT_BLOCKED
        } catch (_: RuntimeException) {
            MonitoringReasonBucket.START_ATTEMPT_BLOCKED
        }
    }

    private suspend fun classifyMissedStart(
        triggerSource: MonitoringTriggerSource,
        expectedBoundaryTs: Long
    ): MonitoringReasonBucket {
        val boundaryExecuted = reliabilityRepository.hasBoundaryExecution(expectedBoundaryTs)
        if (!boundaryExecuted && triggerSource != MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM) {
            return if (triggerSource == MonitoringTriggerSource.APP_LAUNCH_RECONCILE) {
                MonitoringReasonBucket.APP_RESTRICTED_OR_FORCE_STOPPED_SUSPECTED
            } else if (triggerSource == MonitoringTriggerSource.BOOT_OR_TIME_RECONCILE) {
                MonitoringReasonBucket.PROCESS_NOT_STARTED
            } else {
                MonitoringReasonBucket.BOUNDARY_EVENT_DID_NOT_RUN
            }
        }
        return when (triggerSource) {
            MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM -> MonitoringReasonBucket.START_ATTEMPT_BLOCKED
            MonitoringTriggerSource.NIGHT_WINDOW_BACKSTOP,
            MonitoringTriggerSource.PERIODIC_HEALTH_CHECK,
            MonitoringTriggerSource.BOOT_OR_TIME_RECONCILE,
            MonitoringTriggerSource.APP_LAUNCH_RECONCILE -> MonitoringReasonBucket.UNKNOWN
            MonitoringTriggerSource.SCHEDULE -> MonitoringReasonBucket.UNKNOWN
        }
    }

    private fun scheduleNextHealthCheck(now: Long) {
        monitoringHealthScheduler.schedule(now + HEALTH_CHECK_INTERVAL_MS)
    }

    private companion object {
        const val SERVICE_START_VERIFY_DELAY_MS = 1_000L
        const val HEALTH_CHECK_INTERVAL_MS = 15 * 60 * 1_000L
    }
}
