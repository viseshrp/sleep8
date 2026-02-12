package com.sleep8.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sleep8.R
import com.sleep8.ui.main.MainActivity
import com.sleep8.ui.settings.SettingsActivity
import com.sleep8.util.Constants
import com.sleep8.util.PermissionUtils

class NotificationHelper(private val context: Context) {

    fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    fun ensureAlarmRingingChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            Constants.ALARM_RINGING_CHANNEL_ID,
            context.getString(R.string.alarm_channel_ringing),
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(channel)
    }

    fun ensureAlarmScheduledChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            Constants.ALARM_SCHEDULED_CHANNEL_ID,
            context.getString(R.string.alarm_channel_scheduled),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(contentText: String): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(homePendingIntent())
            .setOngoing(true)
            .build()
    }

    fun showWarning(message: String) {
        if (!PermissionUtils.canPostNotifications(context)) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(homePendingIntent())
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.NOTIFICATION_ID + 1, notification)
    }

    fun showExactAlarmWarning() {
        if (!PermissionUtils.canPostNotifications(context)) return
        ensureChannel()
        val settingsIntent = appSettingsPendingIntent()
        val exactAlarmIntent = exactAlarmSettingsPendingIntent()
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.exact_alarm_title))
            .setContentText(context.getString(R.string.exact_alarm_body))
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(settingsIntent)
            .addAction(R.drawable.ic_tile, context.getString(R.string.exact_alarm_action), exactAlarmIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.NOTIFICATION_ID + 2, notification)
    }

    fun showAlarmScheduled(contentText: String, contentIntent: PendingIntent) {
        if (!PermissionUtils.canPostNotifications(context)) return
        ensureAlarmScheduledChannel()
        val notification = NotificationCompat.Builder(context, Constants.ALARM_SCHEDULED_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.alarm_scheduled_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.ALARM_SCHEDULED_NOTIFICATION_ID, notification)
    }

    fun clearAllPendingNotifications() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(Constants.NOTIFICATION_ID)
        manager.cancel(Constants.NOTIFICATION_ID + 1)
        manager.cancel(Constants.NOTIFICATION_ID + 2)
        manager.cancel(Constants.ALARM_SCHEDULED_NOTIFICATION_ID)
        manager.cancel(Constants.ALARM_RINGING_NOTIFICATION_ID)
    }

    private fun homePendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            Constants.PENDING_INTENT_REQUEST_APP_HOME,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun exactAlarmSettingsPendingIntent(): PendingIntent {
        val intent = PermissionUtils.exactAlarmIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            Constants.PENDING_INTENT_REQUEST_EXACT_ALARM_SETTINGS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun appSettingsPendingIntent(): PendingIntent {
        val intent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            Constants.PENDING_INTENT_REQUEST_APP_SETTINGS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
