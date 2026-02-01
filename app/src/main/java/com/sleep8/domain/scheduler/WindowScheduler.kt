package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sleep8.service.receiver.WindowStartReceiver
import com.sleep8.service.receiver.WindowEndReceiver
import com.sleep8.util.Constants

class WindowScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun scheduleWindowStart(windowStartTs: Long) {
        val intent = Intent(context, com.sleep8.service.receiver.WindowStartReceiver::class.java).apply {
            this.action = Constants.ACTION_WINDOW_START
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_WINDOW_START,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, windowStartTs, pendingIntent)
    }

    fun scheduleWindowEnd(windowEndTs: Long) {
        val intent = Intent(context, com.sleep8.service.receiver.WindowEndReceiver::class.java).apply {
            this.action = Constants.ACTION_WINDOW_END
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_WINDOW_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, windowEndTs, pendingIntent)
    }

    fun cancelWindowStart() {
        val intent = Intent(context, com.sleep8.service.receiver.WindowStartReceiver::class.java).apply {
            this.action = Constants.ACTION_WINDOW_START
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_WINDOW_START,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelWindowEnd() {
        val intent = Intent(context, com.sleep8.service.receiver.WindowEndReceiver::class.java).apply {
            this.action = Constants.ACTION_WINDOW_END
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_WINDOW_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
