package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sleep8.service.receiver.MonitoringHealthCheckReceiver
import com.sleep8.util.Constants

class MonitoringHealthScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager
) {

    fun schedule(triggerAtMillis: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_MONITORING_HEALTH_CHECK,
            Intent(context, MonitoringHealthCheckReceiver::class.java).apply {
                action = Constants.ACTION_MONITORING_HEALTH_CHECK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactSafely(triggerAtMillis, pendingIntent)
    }

    fun cancel() {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_MONITORING_HEALTH_CHECK,
            Intent(context, MonitoringHealthCheckReceiver::class.java).apply {
                action = Constants.ACTION_MONITORING_HEALTH_CHECK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun setExactSafely(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            // Exact alarm permission might be unavailable in test/dev environments.
        }
    }
}
