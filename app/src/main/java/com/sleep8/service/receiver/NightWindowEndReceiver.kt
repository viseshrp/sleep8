package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
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

    @androidx.annotation.VisibleForTesting
    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scope.launch {
            handleNightWindowEnd()
            pending?.finish()
        }
    }

    @androidx.annotation.VisibleForTesting
    internal suspend fun handleNightWindowEnd() {
        if (stateHolder.state.value != AppState.DISARMED) {
            // Ring-style: Night Window only gates monitoring, never changes armed state.
            serviceController.stopNightMonitorService()
            stateMachineManager.onNightWindowEnd()
        }
    }
}
