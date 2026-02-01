package com.sleep8.domain.manager

import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.state.StateHolder
import com.sleep8.domain.scheduler.WindowScheduler
import com.sleep8.service.ServiceController
import com.sleep8.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ArmManager(
    private val sessionRepository: SessionRepository,
    private val stateHolder: StateHolder,
    private val serviceController: ServiceController,
    private val windowScheduler: WindowScheduler,
    private val settingsRepository: SettingsRepository
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
        serviceController.startNightMonitorService()
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
        stateHolder.clearPendingCandidate()
        serviceController.stopNightMonitorService()
        windowScheduler.cancelWindowEnd()
        windowScheduler.cancelWindowStart()
        if (source != ArmSource.SCHEDULED) manualOverride = true
        return Result.success(Unit)
    }

    suspend fun handleAutoArm() {
        val settings = settingsRepository.getSettings()
        if (!settings.autoArmEnabled) return
        val start = TimeUtils.parseLocalTime(settings.nightStart)
        val end = TimeUtils.parseLocalTime(settings.nightEnd)
        val now = LocalDateTime.now()
        val window = TimeUtils.calculateNextWindow(now, start, end)
        windowScheduler.scheduleWindowStart(window.startTs)
        windowScheduler.scheduleWindowEnd(window.endTs)
        // If currently within the night window, arm immediately
        if (TimeUtils.isInWindow(now.toLocalTime(), start, end)) {
            arm(ArmSource.SCHEDULED)
            manualOverride = false
        }
    }

    fun onScheduledEvent(type: String) {
        if (!manualOverride) {
            if (type == "start") {
                scope.launch { arm(ArmSource.SCHEDULED) }
            } else if (type == "end") {
                scope.launch { disarm(ArmSource.SCHEDULED) }
            }
        } else {
            manualOverride = false // reset override after scheduled event
        }
    }

    fun observeArmedState(): Flow<Boolean> {
        return stateHolder.state.map { it != com.sleep8.domain.model.AppState.DISARMED }
    }

    fun isArmed(): Boolean {
        return stateHolder.state.value != com.sleep8.domain.model.AppState.DISARMED
    }
}
