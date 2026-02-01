package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep8.domain.manager.ArmManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WindowEndReceiver : BroadcastReceiver() {

    @Inject lateinit var armManager: ArmManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            // Ring-style: Auto-Arm boundaries are authoritative while enabled.
            armManager.disarm()
            pending.finish()
        }
    }
}
