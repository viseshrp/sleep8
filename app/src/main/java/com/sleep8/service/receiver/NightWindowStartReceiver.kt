package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
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
class NightWindowStartReceiver : BroadcastReceiver() {

    @Inject lateinit var stateHolder: StateHolder
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var serviceController: ServiceController
    @Inject lateinit var stateMachineManager: StateMachineManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            if (stateHolder.state.value == AppState.DISARMED) {
                pending.finish()
                return@launch
            }
            val settings = settingsRepository.getSettings()
            val start = TimeUtils.parseLocalTime(settings.nightStart)
            val end = TimeUtils.parseLocalTime(settings.nightEnd)
            val now = LocalDateTime.now()
            val inWindow = TimeUtils.isInWindow(now.toLocalTime(), start, end)
            if (inWindow) {
                // Ring-style: Night Window only gates monitoring, never changes armed state.
                serviceController.startNightMonitorService()
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val screenStillOff = !powerManager.isInteractive
                stateMachineManager.resumePendingConfirmationIfEligible(screenStillOff)
            }
            pending.finish()
        }
    }
}
