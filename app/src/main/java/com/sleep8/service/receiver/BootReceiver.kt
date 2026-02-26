package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.MonitoringTriggerSource
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.util.TimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
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
    @Inject lateinit var confirmOffScheduler: ConfirmOffScheduler
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var stateMachineManager: StateMachineManager
    @Inject lateinit var armManager: ArmManager
    @Inject lateinit var monitoringReliabilityManager: MonitoringReliabilityManager

    @androidx.annotation.VisibleForTesting
    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun onReceive(context: Context, intent: Intent) {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in supportedActions) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scope.launch {
            handleBoot(context)
            pendingResult?.finish()
        }
    }

    @androidx.annotation.VisibleForTesting
    internal suspend fun handleBoot(context: Context) {
        val session = sessionRepository.getActiveSession()
        if (session == null) {
            stateHolder.setActiveSession(null)
            stateHolder.setArmed(false)
            stateHolder.setState(AppState.DISARMED)
            serviceController.stopNightMonitorService()
            return
        }

        val settings = settingsRepository.getSettings()
        val nowMs = System.currentTimeMillis()
        stateHolder.setActiveSession(session)
        // Keep arm/disarm semantics manual-only after reboot/time changes. The previous
        // window may have ended, but that must not force disarm.
        stateHolder.setArmed(true)
        stateHolder.setState(AppState.ARMED_IDLE)

        val now = LocalDateTime.now()
        val nightStart = TimeUtils.parseLocalTime(settings.nightStart)
        val nightEnd = TimeUtils.parseLocalTime(settings.nightEnd)
        val inNightWindow = TimeUtils.isInWindow(now.toLocalTime(), nightStart, nightEnd)
        if (inNightWindow) {
            serviceController.startNightMonitorService()
        } else {
            serviceController.stopNightMonitorService()
        }

        armManager.refreshNightWindowBoundariesIfArmed()
        monitoringReliabilityManager.onTrigger(context, MonitoringTriggerSource.BOOT_OR_TIME_RECONCILE)

        val pendingScreenOffTs = stateHolder.pendingCandidateScreenOffTs.value
        val pendingDeadlineTs = stateHolder.pendingConfirmDeadlineTs.value
        if (pendingScreenOffTs > 0 && pendingDeadlineTs > 0 && inNightWindow) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenOff = !powerManager.isInteractive
            if (screenOff) {
                if (nowMs >= pendingDeadlineTs) {
                    alarmScheduler.scheduleSleepAlarm(pendingScreenOffTs, nowMs)
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

        val scheduled = alarmScheduler.reconcileScheduledAfterBoot()
        if (scheduled != null) {
            val triggerAt = if (scheduled.triggerAt <= nowMs) nowMs + 1_000L else scheduled.triggerAt
            alarmScheduler.rescheduleExisting(scheduled, triggerAt)
        }
    }
}
