package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.OsAlarmCreator
import com.sleep8.domain.scheduler.WindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var stateHolder: StateHolder
    @Inject lateinit var serviceController: ServiceController
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
            armManager.handleAutoArm()
            val session = sessionRepository.getActiveSession()
            if (session == null) {
                pendingResult.finish()
                return@launch
            }

            val settings = settingsRepository.getSettings()
            val now = LocalDateTime.now()

            if (now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() > session.windowEndTs) {
                sessionRepository.endSession(session.id, System.currentTimeMillis())
                stateHolder.setActiveSession(null)
                stateHolder.setState(AppState.DISARMED)
                pendingResult.finish()
                return@launch
            }

            stateHolder.setActiveSession(session)
            stateHolder.setArmed(true)
            stateHolder.setState(AppState.ARMED_IDLE)
            serviceController.startNightMonitorService()
            windowScheduler.scheduleWindowEnd(session.windowEndTs)

            val pendingScreenOffTs = stateHolder.pendingCandidateScreenOffTs.value
            if (pendingScreenOffTs > 0) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val screenOff = !powerManager.isInteractive
                if (screenOff) {
                    val elapsedMs = System.currentTimeMillis() - pendingScreenOffTs
                    if (elapsedMs >= settings.confirmOffMinutes * 60_000L) {
                        osAlarmCreator.createAlarm(pendingScreenOffTs)
                        stateHolder.clearPendingCandidate()
                        stateHolder.setState(AppState.ARMED_ALARM_SET)
                    } else {
                        confirmOffScheduler.scheduleConfirmation(pendingScreenOffTs, settings.confirmOffMinutes)
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
