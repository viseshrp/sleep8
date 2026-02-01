package com.sleep8.domain.manager

import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.state.StateHolder
import com.sleep8.domain.scheduler.WindowEndScheduler
import com.sleep8.service.ServiceController
import com.sleep8.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class ArmManager(
    private val sessionRepository: SessionRepository,
    private val stateHolder: StateHolder,
    private val serviceController: ServiceController,
    private val windowEndScheduler: WindowEndScheduler,
    private val settingsRepository: SettingsRepository
) {

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
        windowEndScheduler.scheduleWindowEnd(window.endTs)

        return Result.success(session)
    }

    suspend fun disarm(): Result<Unit> {
        val session = stateHolder.activeSession.value
        if (session != null) {
            sessionRepository.endSession(session.id, System.currentTimeMillis())
        }
        stateHolder.setActiveSession(null)
        stateHolder.setArmed(false)
        stateHolder.clearPendingCandidate()
        serviceController.stopNightMonitorService()
        windowEndScheduler.cancelWindowEnd()
        return Result.success(Unit)
    }

    fun observeArmedState(): Flow<Boolean> {
        return stateHolder.state.map { it != com.sleep8.domain.model.AppState.DISARMED }
    }

    fun isArmed(): Boolean {
        return stateHolder.state.value != com.sleep8.domain.model.AppState.DISARMED
    }
}
