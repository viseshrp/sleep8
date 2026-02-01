package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BackstopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Internal backstop hook; no user-facing behavior.
    }
}
