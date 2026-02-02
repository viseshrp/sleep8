package com.sleep8.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.sleep8.R
import com.sleep8.util.Constants

class AlarmNotificationFactory(private val context: Context) {

    fun buildRingingNotification(
        alarmIntent: PendingIntent,
        contentIntent: PendingIntent,
        dismissIntent: PendingIntent
    ): Notification {
        val builder = NotificationCompat.Builder(context, Constants.ALARM_RINGING_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.alarm_notification_title))
            .setContentText(context.getString(R.string.alarm_notification_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(alarmIntent, true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_tile, context.getString(R.string.alarm_dismiss), dismissIntent)
            .setOngoing(true)

        return builder.build()
    }
}
