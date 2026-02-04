package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.sleep8.domain.manager.StateMachineManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmationAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var stateMachineManager: StateMachineManager

    @androidx.annotation.VisibleForTesting
    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scope.launch {
            handleConfirmation(context)
            pending?.finish()
        }
    }

    @androidx.annotation.VisibleForTesting
    internal suspend fun handleConfirmation(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenStillOff = !powerManager.isInteractive
        stateMachineManager.onConfirmationTimerExpired(screenStillOff)
    }
}
