package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep8.domain.manager.ArmManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WindowStartReceiver : BroadcastReceiver() {
    @Inject lateinit var armManager: ArmManager
    override fun onReceive(context: Context, intent: Intent) {
        armManager.onScheduledEvent("start")
    }
}
