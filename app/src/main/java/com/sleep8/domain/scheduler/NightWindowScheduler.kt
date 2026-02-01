package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sleep8.service.receiver.NightWindowEndReceiver
import com.sleep8.service.receiver.NightWindowStartReceiver
import com.sleep8.util.Constants

class NightWindowScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun scheduleWindowStart(windowStartTs: Long) {
        val intent = Intent(context, NightWindowStartReceiver::class.java).apply {
            action = Constants.ACTION_NIGHT_WINDOW_START
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_START,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, windowStartTs, pendingIntent)
    }

    fun scheduleWindowEnd(windowEndTs: Long) {
        val intent = Intent(context, NightWindowEndReceiver::class.java).apply {
            action = Constants.ACTION_NIGHT_WINDOW_END
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, windowEndTs, pendingIntent)
    }

    fun cancelWindowStart() {
        val intent = Intent(context, NightWindowStartReceiver::class.java).apply {
            action = Constants.ACTION_NIGHT_WINDOW_START
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_START,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelWindowEnd() {
        val intent = Intent(context, NightWindowEndReceiver::class.java).apply {
            action = Constants.ACTION_NIGHT_WINDOW_END
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
