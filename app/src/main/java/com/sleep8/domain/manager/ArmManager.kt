package com.sleep8.domain.manager

import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.NightWindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ArmManager(
    private val sessionRepository: SessionRepository,
    private val stateHolder: StateHolder,
    private val serviceController: ServiceController,
    private val settingsRepository: SettingsRepository,
    private val nightWindowScheduler: NightWindowScheduler,
    private val confirmOffScheduler: ConfirmOffScheduler,
    private val monitoringReliabilityManager: MonitoringReliabilityManager,
    private val notificationHelper: NotificationHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun arm(source: ArmSource): Result<ArmSession> {
        if (stateHolder.state.value != com.sleep8.domain.model.AppState.DISARMED) {
            val existing = stateHolder.activeSession.value ?: sessionRepository.getActiveSession()?.also {
                stateHolder.setActiveSession(it)
            }
            return Result.success(existing ?: ArmSession(0, 0, null, 0, 0, source))
        }
        stateHolder.clearLastScreenOffTs()
        val settings = settingsRepository.getSettings()
        val start = TimeUtils.parseLocalTime(settings.nightStart)
        val end = TimeUtils.parseLocalTime(settings.nightEnd)
        val window = TimeUtils.calculateNextWindow(LocalDateTime.now(), start, end)
        val session = sessionRepository.createSession(
            armedAt = System.currentTimeMillis(),
            windowStartTs = window.startTs,
            windowEndTs = window.endTs,
            source = source
        )
        stateHolder.setActiveSession(session)
        stateHolder.setArmed(true)
        refreshNightWindowBoundariesIfArmed()
        return Result.success(session)
    }

    suspend fun disarm(source: ArmSource = ArmSource.APP_BUTTON): Result<Unit> {
        val session = stateHolder.activeSession.value ?: sessionRepository.getActiveSession()
        if (session != null) {
            sessionRepository.endSession(session.id, System.currentTimeMillis())
        }
        stateHolder.setActiveSession(null)
        stateHolder.setArmed(false)
        stateHolder.clearPendingCandidate()
        stateHolder.clearLastScreenOffTs()
        confirmOffScheduler.cancelConfirmation()
        serviceController.stopNightMonitorService()
        nightWindowScheduler.cancelWindowStart()
        nightWindowScheduler.cancelWindowEnd()
        nightWindowScheduler.cancelWindowStartBackstops()
        monitoringReliabilityManager.onNightWindowEnded()
        notificationHelper.clearAllPendingNotifications()
        return Result.success(Unit)
    }

    suspend fun refreshNightWindowBoundariesIfArmed() {
        if (!isArmed()) {
            serviceController.stopNightMonitorService()
            nightWindowScheduler.cancelWindowStart()
            nightWindowScheduler.cancelWindowEnd()
            nightWindowScheduler.cancelWindowStartBackstops()
            monitoringReliabilityManager.onNightWindowEnded()
            return
        }
        val settings = settingsRepository.getSettings()
        scheduleNightWindowBoundaries(settings.nightStart, settings.nightEnd)
    }

    private fun scheduleNightWindowBoundaries(nightStart: String, nightEnd: String) {
        val start = TimeUtils.parseLocalTime(nightStart)
        val end = TimeUtils.parseLocalTime(nightEnd)
        val now = LocalDateTime.now()
        val inWindow = TimeUtils.isInWindow(now.toLocalTime(), start, end)
        val currentOrNext = TimeUtils.calculateNextWindow(now, start, end)
        if (inWindow) {
            serviceController.startNightMonitorService()
            notificationHelper.showMonitoringIdleNow()
            nightWindowScheduler.scheduleWindowEnd(currentOrNext.endTs)
            val nextStart = TimeUtils.calculateNextWindow(
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(currentOrNext.endTs + 60_000L),
                    ZoneId.systemDefault()
                ),
                start,
                end
            )
            nightWindowScheduler.scheduleWindowStart(nextStart.startTs)
            nightWindowScheduler.scheduleWindowStartBackstops(nextStart.startTs)
            scope.launch {
                monitoringReliabilityManager.recordNightWindowStartSchedule(nextStart.startTs)
            }
        } else {
            serviceController.stopNightMonitorService()
            nightWindowScheduler.scheduleWindowStart(currentOrNext.startTs)
            nightWindowScheduler.scheduleWindowEnd(currentOrNext.endTs)
            nightWindowScheduler.scheduleWindowStartBackstops(currentOrNext.startTs)
            scope.launch {
                monitoringReliabilityManager.recordNightWindowStartSchedule(currentOrNext.startTs)
            }
        }
    }

    fun observeArmedState(): Flow<Boolean> {
        return stateHolder.state.map { it != com.sleep8.domain.model.AppState.DISARMED }
    }

    fun isArmed(): Boolean {
        return stateHolder.state.value != com.sleep8.domain.model.AppState.DISARMED
    }
}
