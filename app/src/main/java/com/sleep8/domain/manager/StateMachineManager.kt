package com.sleep8.domain.manager

import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.ScreenEventType
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.util.TimeUtils
import java.time.Instant
import java.time.LocalDateTime

class StateMachineManager(
    private val stateHolder: StateHolder,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val alarmRepository: AlarmRepository,
    private val confirmOffScheduler: ConfirmOffScheduler,
    private val alarmScheduler: AlarmScheduler
) {

    val currentState: AppState
        get() = stateHolder.state.value

    val pendingCandidateTime: Instant?
        get() = stateHolder.pendingCandidateScreenOffTs.value.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }

    suspend fun onScreenOff(screenOffTime: Instant) {
        if (currentState == AppState.DISARMED) return

        val settings = settingsRepository.getSettings()
        val start = TimeUtils.parseLocalTime(settings.nightStart)
        val end = TimeUtils.parseLocalTime(settings.nightEnd)
        val nowLocal = LocalDateTime.ofInstant(screenOffTime, java.time.ZoneId.systemDefault()).toLocalTime()
        val inWindow = TimeUtils.isInWindow(nowLocal, start, end)

        val session = stateHolder.activeSession.value ?: return
        sessionRepository.insertScreenEvent(session.id, ScreenEventType.SCREEN_OFF, screenOffTime.toEpochMilli())
        stateHolder.setLastScreenOffTs(screenOffTime.toEpochMilli())

        if (!inWindow) {
            return
        }

        val deadline = screenOffTime.toEpochMilli() + settings.confirmOffMinutes * 60_000L
        stateHolder.setPendingCandidate(screenOffTime.toEpochMilli(), deadline)
        stateHolder.setState(AppState.ARMED_PENDING_CONFIRM)
        confirmOffScheduler.scheduleConfirmation(screenOffTime.toEpochMilli(), settings.confirmOffMinutes)
    }

    suspend fun onScreenOn() {
        if (currentState == AppState.DISARMED) return

        val session = stateHolder.activeSession.value ?: return
        sessionRepository.insertScreenEvent(session.id, ScreenEventType.SCREEN_ON, System.currentTimeMillis())

        if (currentState == AppState.ARMED_PENDING_CONFIRM) {
            confirmOffScheduler.cancelConfirmation()
            stateHolder.clearPendingCandidate()
            stateHolder.setState(AppState.ARMED_IDLE)
        }
    }

    suspend fun onConfirmationTimerExpired(screenStillOff: Boolean) {
        if (currentState != AppState.ARMED_PENDING_CONFIRM) return

        if (!screenStillOff) {
            confirmOffScheduler.cancelConfirmation()
            stateHolder.clearPendingCandidate()
            stateHolder.setState(AppState.ARMED_IDLE)
            return
        }

        val screenOffTs = stateHolder.pendingCandidateScreenOffTs.value
        if (screenOffTs > 0) {
            alarmScheduler.scheduleSleepAlarm(screenOffTs, System.currentTimeMillis())
        }
        stateHolder.clearPendingCandidate()
        stateHolder.setState(AppState.ARMED_ALARM_SET)

        val session = stateHolder.activeSession.value
        if (session != null) {
            alarmRepository.getAlarmsForSession(session.id)
        }
    }

    suspend fun onNightWindowEnd() {
        if (currentState == AppState.DISARMED) return
        confirmOffScheduler.cancelConfirmationTimerOnly()
        if (currentState == AppState.ARMED_PENDING_CONFIRM) {
            stateHolder.setState(AppState.ARMED_IDLE)
        }
    }

    suspend fun resumePendingConfirmationIfEligible(screenStillOff: Boolean) {
        if (currentState == AppState.DISARMED) return
        val screenOffTs = stateHolder.pendingCandidateScreenOffTs.value
        val deadlineTs = stateHolder.pendingConfirmDeadlineTs.value
        if (screenOffTs <= 0 || deadlineTs <= 0) return

        if (!screenStillOff) {
            stateHolder.setState(AppState.ARMED_IDLE)
            return
        }

        val now = System.currentTimeMillis()
        if (now >= deadlineTs) {
            alarmScheduler.scheduleSleepAlarm(screenOffTs, System.currentTimeMillis())
            stateHolder.clearPendingCandidate()
            stateHolder.setState(AppState.ARMED_ALARM_SET)
            return
        }

        confirmOffScheduler.scheduleConfirmationAt(screenOffTs, deadlineTs)
        stateHolder.setState(AppState.ARMED_PENDING_CONFIRM)
    }

    fun disarm() {
        confirmOffScheduler.cancelConfirmation()
        stateHolder.clearPendingCandidate()
        stateHolder.setState(AppState.DISARMED)
    }
}
