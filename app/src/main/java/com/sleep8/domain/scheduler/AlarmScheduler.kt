package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleep8.R
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.service.receiver.AlarmReceiver
import com.sleep8.util.AlarmIntents
import com.sleep8.util.Constants
import com.sleep8.util.PermissionUtils
import com.sleep8.util.TimeUtils
import java.time.Instant

class AlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val appPreferences: AppPreferences,
    private val notificationHelper: NotificationHelper
) {

    suspend fun scheduleSleepAlarm(screenOffTs: Long, confirmedAt: Long): AlarmRecord {
        val settings = settingsRepository.getSettings()
        val durationMinutes = settings.alarmOffsetHours * 60
        val triggerAt = Instant.ofEpochMilli(screenOffTs)
            .plusSeconds(durationMinutes * 60L)
            .toEpochMilli()
        return scheduleAlarm(
            screenOffTs = screenOffTs,
            confirmedAt = confirmedAt,
            triggerAt = triggerAt,
            durationUsedMinutes = durationMinutes,
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
            durationUsedMinutes = snoozeMinutes,
            source = AlarmSource.SNOOZE
        )
    }

    suspend fun cancelScheduledAlarm() {
        val existing = alarmRepository.getLatestScheduledRecord() ?: return
        val pendingIntent = buildAlarmPendingIntent(existing.id, existing.requestCode, existing.alarmInstanceId)
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleExisting(record: AlarmRecord, triggerAt: Long) {
        if (!PermissionUtils.canScheduleExactAlarms(context)) {
            Log.e("AlarmScheduler", "Exact alarm permission missing; cannot reschedule alarm after reboot.")
            notificationHelper.showExactAlarmWarning()
        }
        val operation = buildAlarmPendingIntent(record.id, record.requestCode, record.alarmInstanceId)
        val showIntent = AlarmIntents.alarmHistoryPendingIntent(
            context,
            Constants.PENDING_INTENT_REQUEST_ALARM_ACTION
        )
        alarmManager.setAlarmClock(AlarmClockInfo(triggerAt, showIntent), operation)
    }

    private suspend fun scheduleAlarm(
        screenOffTs: Long,
        confirmedAt: Long,
        triggerAt: Long,
        durationUsedMinutes: Int,
        source: AlarmSource
    ): AlarmRecord {
        val scheduledAt = System.currentTimeMillis()
        cancelScheduledAlarm()
        val instanceId = appPreferences.nextAlarmInstanceId()
        val requestCode = (instanceId % Int.MAX_VALUE).toInt()

        val record = AlarmRecord(
            id = 0L,
            sessionId = appPreferences.activeSessionId,
            screenOffTs = screenOffTs,
            confirmedAt = confirmedAt,
            scheduledAt = scheduledAt,
            triggerAt = triggerAt,
            durationUsedMinutes = durationUsedMinutes,
            alarmInstanceId = instanceId,
            requestCode = requestCode,
            scheduledViaAlarmClock = true,
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
        }

        val operation = buildAlarmPendingIntent(alarmId, requestCode, instanceId)
        val showIntent = AlarmIntents.alarmHistoryPendingIntent(
            context,
            Constants.PENDING_INTENT_REQUEST_ALARM_ACTION
        )
        alarmManager.setAlarmClock(AlarmClockInfo(triggerAt, showIntent), operation)

        val scheduledText = context.getString(
            R.string.alarm_scheduled_body,
            TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(triggerAt))
        )
        notificationHelper.showAlarmScheduled(
            scheduledText,
            AlarmIntents.alarmDetailPendingIntent(
                context,
                alarmId.toInt(),
                alarmId
            )
        )

        return record.copy(id = alarmId)
    }

    private fun buildAlarmPendingIntent(alarmId: Long, requestCode: Int, alarmInstanceId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_RING
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            putExtra(Constants.EXTRA_ALARM_INSTANCE_ID, alarmInstanceId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
