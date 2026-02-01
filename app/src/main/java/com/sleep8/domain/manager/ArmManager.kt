package com.sleep8.domain.manager

import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.domain.scheduler.NightWindowScheduler
import com.sleep8.domain.scheduler.WindowScheduler
import com.sleep8.service.ServiceController
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
    private val windowScheduler: WindowScheduler,
    private val settingsRepository: SettingsRepository,
    private val nightWindowScheduler: NightWindowScheduler,
    private val confirmOffScheduler: ConfirmOffScheduler
) {
    private var manualOverride: Boolean = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun arm(source: ArmSource): Result<ArmSession> {
        if (stateHolder.state.value != com.sleep8.domain.model.AppState.DISARMED) {
            return Result.success(stateHolder.activeSession.value ?: ArmSession(0, 0, null, 0, 0, source))
        }
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
        windowScheduler.scheduleWindowEnd(window.endTs)
        windowScheduler.scheduleWindowStart(window.startTs)
        if (source != ArmSource.SCHEDULED) manualOverride = true
        return Result.success(session)
    }

    suspend fun disarm(source: ArmSource = ArmSource.APP_BUTTON): Result<Unit> {
        val session = stateHolder.activeSession.value
        if (session != null) {
            sessionRepository.endSession(session.id, System.currentTimeMillis())
        }
        stateHolder.setActiveSession(null)
        stateHolder.setArmed(false)
        if (source != ArmSource.SCHEDULED) {
            stateHolder.clearPendingCandidate()
            confirmOffScheduler.cancelConfirmation()
        } else {
            confirmOffScheduler.cancelConfirmationTimerOnly()
        }
        serviceController.stopNightMonitorService()
        nightWindowScheduler.cancelWindowStart()
        nightWindowScheduler.cancelWindowEnd()
        windowScheduler.cancelWindowEnd()
        windowScheduler.cancelWindowStart()
        if (source != ArmSource.SCHEDULED) manualOverride = true
        return Result.success(Unit)
    }

    suspend fun handleAutoArm() {
        val settings = settingsRepository.getSettings()
        if (!settings.autoArmEnabled) return
        val start = TimeUtils.parseLocalTime(settings.autoArmStart)
        val end = TimeUtils.parseLocalTime(settings.autoArmEnd)
        val now = LocalDateTime.now()
        val window = TimeUtils.calculateNextWindow(now, start, end)
        windowScheduler.scheduleWindowStart(window.startTs)
        windowScheduler.scheduleWindowEnd(window.endTs)
        // If currently within the auto-arm window, arm immediately
        if (TimeUtils.isInWindow(now.toLocalTime(), start, end)) {
            arm(ArmSource.SCHEDULED)
            manualOverride = false
        }
    }

    suspend fun updateAutoArmEnabled(enabled: Boolean) {
        if (enabled) {
            handleAutoArm()
        } else {
            windowScheduler.cancelWindowStart()
            windowScheduler.cancelWindowEnd()
            manualOverride = false
        }
    }

    suspend fun onScheduledEvent(type: String) {
        manualOverride = false // reset override at the scheduled boundary
        if (type == "start") {
            arm(ArmSource.SCHEDULED)
        } else if (type == "end") {
            disarm(ArmSource.SCHEDULED)
        }
    }

    suspend fun syncAutoArmStateNow() {
        val settings = settingsRepository.getSettings()
        if (!settings.autoArmEnabled) return
        if (manualOverride) return

        val start = TimeUtils.parseLocalTime(settings.autoArmStart)
        val end = TimeUtils.parseLocalTime(settings.autoArmEnd)
        val now = LocalDateTime.now()
        val inAutoWindow = TimeUtils.isInWindow(now.toLocalTime(), start, end)
        val session = stateHolder.activeSession.value

        if (inAutoWindow) {
            if (!isArmed()) {
                arm(ArmSource.SCHEDULED)
            }
        } else {
            if (session?.source == ArmSource.SCHEDULED) {
                disarm(ArmSource.SCHEDULED)
            }
        }
    }

    suspend fun refreshNightWindowBoundariesIfArmed() {
        if (!isArmed()) {
            serviceController.stopNightMonitorService()
            nightWindowScheduler.cancelWindowStart()
            nightWindowScheduler.cancelWindowEnd()
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
        } else {
            serviceController.stopNightMonitorService()
            nightWindowScheduler.scheduleWindowStart(currentOrNext.startTs)
            nightWindowScheduler.scheduleWindowEnd(currentOrNext.endTs)
        }
    }

    fun observeArmedState(): Flow<Boolean> {
        return stateHolder.state.map { it != com.sleep8.domain.model.AppState.DISARMED }
    }

    fun isArmed(): Boolean {
        return stateHolder.state.value != com.sleep8.domain.model.AppState.DISARMED
    }
}
