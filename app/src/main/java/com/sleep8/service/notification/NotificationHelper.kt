package com.sleep8.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.sleep8.R
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

    fun ensureAlarmChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            Constants.ALARM_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(contentText: String): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .build()
    }

    fun showWarning(message: String) {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(false)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.NOTIFICATION_ID + 1, notification)
    }

    fun showExactAlarmWarning() {
        ensureChannel()
        val intent = PermissionUtils.exactAlarmIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.PENDING_INTENT_REQUEST_ALARM_ACTION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.exact_alarm_title))
            .setContentText(context.getString(R.string.exact_alarm_body))
            .setSmallIcon(R.drawable.ic_tile)
            .addAction(R.drawable.ic_tile, context.getString(R.string.exact_alarm_action), pendingIntent)
            .setOngoing(false)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.NOTIFICATION_ID + 2, notification)
    }
}
