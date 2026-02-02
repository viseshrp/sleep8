package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.service.receiver.AlarmReceiver
import com.sleep8.util.Constants
import com.sleep8.util.PermissionUtils
import java.time.Instant

class AlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val alarmRepository: AlarmRepository,
    private val appPreferences: AppPreferences,
    private val notificationHelper: NotificationHelper
) {

    suspend fun scheduleSleepAlarm(screenOffTs: Long, confirmedAt: Long): AlarmRecord {
        val triggerAt = Instant.ofEpochMilli(screenOffTs)
            .plusSeconds(Constants.ALARM_OFFSET_HOURS * 3600L)
            .toEpochMilli()
        return scheduleAlarm(
            screenOffTs = screenOffTs,
            confirmedAt = confirmedAt,
            triggerAt = triggerAt,
            source = AlarmSource.SLEEP_AUTOMATION
        )
    }

    suspend fun scheduleSnooze(alarmId: Long, snoozeMinutes: Int): AlarmRecord? {
        val original = alarmRepository.getRecord(alarmId) ?: return null
        val snoozedUntil = System.currentTimeMillis() + snoozeMinutes * 60_000L
        alarmRepository.markSnoozed(alarmId, snoozedUntil)
        return scheduleAlarm(
            screenOffTs = original.screenOffTs,
            confirmedAt = original.confirmedAt,
            triggerAt = snoozedUntil,
            source = AlarmSource.SNOOZE
        )
    }

    fun cancelScheduledAlarm() {
        val pendingIntent = buildAlarmPendingIntent(alarmId = 0L)
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleImmediate(alarmId: Long, triggerAt: Long) {
        if (!PermissionUtils.canScheduleExactAlarms(context)) {
            Log.e("AlarmScheduler", "Exact alarm permission missing; cannot reschedule alarm after reboot.")
            notificationHelper.showExactAlarmWarning()
            return
        }
        val pendingIntent = buildAlarmPendingIntent(alarmId)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private suspend fun scheduleAlarm(
        screenOffTs: Long,
        confirmedAt: Long,
        triggerAt: Long,
        source: AlarmSource
    ): AlarmRecord {
        val scheduledAt = System.currentTimeMillis()
        cancelScheduledAlarm()

        val record = AlarmRecord(
            id = 0L,
            sessionId = appPreferences.activeSessionId,
            screenOffTs = screenOffTs,
            confirmedAt = confirmedAt,
            scheduledAt = scheduledAt,
            triggerAt = triggerAt,
            source = source,
            status = AlarmStatus.SCHEDULED,
            firedAt = null,
            dismissedAt = null,
            snoozedUntil = null
        )
        val alarmId = alarmRepository.insertRecord(record)

        if (!PermissionUtils.canScheduleExactAlarms(context)) {
            Log.e("AlarmScheduler", "Exact alarm permission missing; cannot schedule exact alarm.")
            notificationHelper.showExactAlarmWarning()
            return record.copy(id = alarmId)
        }

        val pendingIntent = buildAlarmPendingIntent(alarmId)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        return record.copy(id = alarmId)
    }

    private fun buildAlarmPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_RING
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
