package com.sleep8.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ServiceControllerImpl(private val context: Context) : ServiceController {

    override fun startNightMonitorService() {
        val intent = Intent(context, NightMonitorService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopNightMonitorService() {
        val intent = Intent(context, NightMonitorService::class.java)
        context.stopService(intent)
    }
}
