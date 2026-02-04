package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep8.domain.manager.ArmManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WindowStartReceiver : BroadcastReceiver() {
    @Inject lateinit var armManager: ArmManager

    @androidx.annotation.VisibleForTesting
    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scope.launch {
            handleWindowStart()
            pending?.finish()
        }
    }

    @androidx.annotation.VisibleForTesting
    internal fun handleWindowStart() {
        // Ring-style: Auto-Arm boundaries are authoritative while enabled.
        armManager.onScheduledEvent("start")
    }
}
