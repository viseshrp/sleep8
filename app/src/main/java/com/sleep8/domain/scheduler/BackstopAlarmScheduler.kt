package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sleep8.service.receiver.BackstopAlarmReceiver
import com.sleep8.util.Constants

class BackstopAlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager
) {

    fun scheduleBackstop(alarmTimeMillis: Long) {
        val intent = Intent(context, BackstopAlarmReceiver::class.java).apply {
            action = Constants.ACTION_BACKSTOP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_BACKSTOP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent)
    }
}
