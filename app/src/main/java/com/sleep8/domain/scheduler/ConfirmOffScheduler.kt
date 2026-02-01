package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.service.receiver.ConfirmationAlarmReceiver
import com.sleep8.util.Constants

class ConfirmOffScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val appPreferences: AppPreferences
) {

    fun scheduleConfirmation(screenOffTs: Long, confirmMinutes: Int) {
        val triggerAt = screenOffTs + confirmMinutes * 60_000L
        val intent = Intent(context, ConfirmationAlarmReceiver::class.java).apply {
            action = Constants.ACTION_CONFIRMATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_CONFIRM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        appPreferences.pendingConfirmDeadlineTs = triggerAt
        appPreferences.pendingCandidateScreenOffTs = screenOffTs
    }

    fun cancelConfirmation() {
        val intent = Intent(context, ConfirmationAlarmReceiver::class.java).apply {
            action = Constants.ACTION_CONFIRMATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_CONFIRM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        appPreferences.clearPendingConfirmation()
    }
}
