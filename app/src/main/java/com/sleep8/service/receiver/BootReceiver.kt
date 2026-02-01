package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.OsAlarmCreator
import com.sleep8.domain.scheduler.WindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.util.TimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var stateHolder: StateHolder
    @Inject lateinit var serviceController: ServiceController
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var confirmOffScheduler: ConfirmOffScheduler
    @Inject lateinit var osAlarmCreator: OsAlarmCreator
    @Inject lateinit var windowScheduler: WindowScheduler
    @Inject lateinit var stateMachineManager: StateMachineManager
    @Inject lateinit var armManager: ArmManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        scope.launch {
            val session = sessionRepository.getActiveSession()
            val settings = settingsRepository.getSettings()
            val now = LocalDateTime.now()
            val autoStart = TimeUtils.parseLocalTime(settings.autoArmStart)
            val autoEnd = TimeUtils.parseLocalTime(settings.autoArmEnd)
            val manualOverrideActive = appPreferences.manualOverrideActive
            val shouldBeArmedNow = settings.autoArmEnabled &&
                !manualOverrideActive &&
                TimeUtils.isInWindow(now.toLocalTime(), autoStart, autoEnd)

            if (settings.autoArmEnabled) {
                val autoWindow = TimeUtils.calculateNextWindow(now, autoStart, autoEnd)
                windowScheduler.scheduleWindowStart(autoWindow.startTs)
                windowScheduler.scheduleWindowEnd(autoWindow.endTs)
            }

            if (settings.autoArmEnabled) {
                if (!shouldBeArmedNow) {
                    if (session != null) {
                        sessionRepository.endSession(session.id, System.currentTimeMillis())
                    }
                    stateHolder.setActiveSession(null)
                    stateHolder.setArmed(false)
                    stateHolder.setState(AppState.DISARMED)
                    serviceController.stopNightMonitorService()
                    confirmOffScheduler.cancelConfirmationTimerOnly()
                    pendingResult.finish()
                    return@launch
                }

                if (stateHolder.state.value != AppState.DISARMED || session != null) {
                    session?.let { sessionRepository.endSession(it.id, System.currentTimeMillis()) }
                    stateHolder.setActiveSession(null)
                }
                armManager.arm(com.sleep8.domain.model.ArmSource.SCHEDULED)
            }

            val activeSession = sessionRepository.getActiveSession()
            if (activeSession == null) {
                if (!settings.autoArmEnabled) {
                    stateHolder.setActiveSession(null)
                    stateHolder.setArmed(false)
                    stateHolder.setState(AppState.DISARMED)
                    serviceController.stopNightMonitorService()
                }
                pendingResult.finish()
                return@launch
            }

            if (now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() > activeSession.windowEndTs) {
                sessionRepository.endSession(activeSession.id, System.currentTimeMillis())
                stateHolder.setActiveSession(null)
                stateHolder.setState(AppState.DISARMED)
                pendingResult.finish()
                return@launch
            }

            stateHolder.setActiveSession(activeSession)
            stateHolder.setArmed(true)
            stateHolder.setState(AppState.ARMED_IDLE)
            val nightStart = TimeUtils.parseLocalTime(settings.nightStart)
            val nightEnd = TimeUtils.parseLocalTime(settings.nightEnd)
            val inNightWindow = TimeUtils.isInWindow(now.toLocalTime(), nightStart, nightEnd)
            if (inNightWindow) {
                serviceController.startNightMonitorService()
            } else {
                serviceController.stopNightMonitorService()
            }
            armManager.refreshNightWindowBoundariesIfArmed()

            val pendingScreenOffTs = stateHolder.pendingCandidateScreenOffTs.value
            val pendingDeadlineTs = stateHolder.pendingConfirmDeadlineTs.value
            if (pendingScreenOffTs > 0 && pendingDeadlineTs > 0 && inNightWindow) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val screenOff = !powerManager.isInteractive
                if (screenOff) {
                    val nowMs = System.currentTimeMillis()
                    if (nowMs >= pendingDeadlineTs) {
                        osAlarmCreator.createAlarm(pendingScreenOffTs)
                        stateHolder.clearPendingCandidate()
                        stateHolder.setState(AppState.ARMED_ALARM_SET)
                    } else {
                        confirmOffScheduler.scheduleConfirmationAt(pendingScreenOffTs, pendingDeadlineTs)
                        stateHolder.setState(AppState.ARMED_PENDING_CONFIRM)
                    }
                } else {
                    stateMachineManager.onScreenOn()
                }
            }
            pendingResult.finish()
        }
    }
}
