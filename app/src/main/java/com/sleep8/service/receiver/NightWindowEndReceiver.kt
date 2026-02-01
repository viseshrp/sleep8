package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NightWindowEndReceiver : BroadcastReceiver() {

    @Inject lateinit var stateHolder: StateHolder
    @Inject lateinit var serviceController: ServiceController
    @Inject lateinit var stateMachineManager: StateMachineManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            if (stateHolder.state.value != AppState.DISARMED) {
                serviceController.stopNightMonitorService()
                stateMachineManager.onNightWindowEnd()
            }
            pending.finish()
        }
    }
}
