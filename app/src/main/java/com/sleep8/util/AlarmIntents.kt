package com.sleep8.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

object AlarmIntents {

    fun alarmHistoryUri(): Uri = Uri.parse("sleep8://alarms")

    fun alarmDetailUri(alarmId: Long): Uri = Uri.parse("sleep8://alarm/$alarmId")

    fun parseAlarmId(uri: Uri?): Long? {
        if (uri == null) return null
        if (uri.host != "alarm") return null
        return uri.lastPathSegment?.toLongOrNull()
    }

    fun alarmHistoryIntent(context: Context): Intent {
        return Intent(Intent.ACTION_VIEW, alarmHistoryUri()).apply {
            setPackage(context.packageName)
        }
    }

    fun alarmDetailIntent(context: Context, alarmId: Long): Intent {
        return Intent(Intent.ACTION_VIEW, alarmDetailUri(alarmId)).apply {
            setPackage(context.packageName)
        }
    }

    fun alarmHistoryPendingIntent(context: Context, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            alarmHistoryIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun alarmDetailPendingIntent(context: Context, requestCode: Int, alarmId: Long): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            alarmDetailIntent(context, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
