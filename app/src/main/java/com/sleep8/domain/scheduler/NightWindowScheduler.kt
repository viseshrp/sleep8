package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sleep8.service.receiver.NightWindowEndReceiver
import com.sleep8.service.receiver.NightWindowBackstopReceiver
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
        setExactSafely(windowStartTs, pendingIntent)
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
        setExactSafely(windowEndTs, pendingIntent)
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

    fun scheduleWindowStartBackstops(windowStartTs: Long) {
        scheduleBackstop(
            requestCode = Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_BACKSTOP_SOON,
            triggerAtTs = windowStartTs + BACKSTOP_SOON_OFFSET_MS
        )
        scheduleBackstop(
            requestCode = Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_BACKSTOP_LATE,
            triggerAtTs = windowStartTs + BACKSTOP_LATE_OFFSET_MS
        )
    }

    fun cancelWindowStartBackstops() {
        cancelBackstop(Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_BACKSTOP_SOON)
        cancelBackstop(Constants.PENDING_INTENT_REQUEST_NIGHT_WINDOW_BACKSTOP_LATE)
    }

    private fun setExactSafely(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            // Exact alarm permission might be unavailable in test/dev environments.
        }
    }

    private fun scheduleBackstop(requestCode: Int, triggerAtTs: Long) {
        val intent = Intent(context, NightWindowBackstopReceiver::class.java).apply {
            action = Constants.ACTION_NIGHT_WINDOW_BACKSTOP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactSafely(triggerAtTs, pendingIntent)
    }

    private fun cancelBackstop(requestCode: Int) {
        val intent = Intent(context, NightWindowBackstopReceiver::class.java).apply {
            action = Constants.ACTION_NIGHT_WINDOW_BACKSTOP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private companion object {
        const val BACKSTOP_SOON_OFFSET_MS = 2 * 60 * 1_000L
        const val BACKSTOP_LATE_OFFSET_MS = 10 * 60 * 1_000L
    }
}
