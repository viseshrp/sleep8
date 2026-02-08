package com.sleep8.service

import android.content.Context
import com.sleep8.util.PermissionUtils

class MonitoringRuntimeInspector {
    fun isMonitoringActive(context: Context): Boolean {
        return PermissionUtils.isServiceRunning(context, NightMonitorService::class.java)
    }
}
