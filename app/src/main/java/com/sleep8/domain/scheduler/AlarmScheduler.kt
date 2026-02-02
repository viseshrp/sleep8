package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleep8.R
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.AlarmCancelReason
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.service.receiver.AlarmReceiver
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
        cancelScheduledAlarms(AlarmCancelReason.REPLACED_BY_NEW_ALARM)
        val settings = settingsRepository.getSettings()
        val durationMinutes = com.sleep8.util.AlarmDurationValidator.clamp(settings.alarmDurationMinutes)
        val baseTs = if (durationMinutes == 0) confirmedAt else screenOffTs
        val triggerAt = Instant.ofEpochMilli(baseTs)
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
        cancelScheduledAlarms(AlarmCancelReason.SNOOZE_REPLACE)
        val snoozedUntil = System.currentTimeMillis() + snoozeMinutes * 60_000L
        alarmRepository.markSnoozed(alarmId, System.currentTimeMillis(), snoozedUntil)
        return scheduleAlarm(
            screenOffTs = original.screenOffTs,
            confirmedAt = original.confirmedAt,
            triggerAt = snoozedUntil,
            durationUsedMinutes = snoozeMinutes,
            source = AlarmSource.SNOOZE
        )
    }

    fun rescheduleExisting(record: AlarmRecord, triggerAt: Long) {
        if (!PermissionUtils.canScheduleExactAlarms(context)) {
            Log.e("AlarmScheduler", "Exact alarm permission missing; cannot reschedule alarm after reboot.")
            notificationHelper.showExactAlarmWarning()
        }
        val operation = buildAlarmPendingIntent(record.id, record.requestCode, record.alarmInstanceId)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }

    suspend fun cancelActiveAlarms(reason: AlarmCancelReason) {
        cancelScheduledAlarms(reason)
    }

    suspend fun reconcileScheduledAfterBoot(): AlarmRecord? {
        val scheduled = alarmRepository.getScheduledRecords()
        if (scheduled.isEmpty()) {
            clearActiveAlarmIdentity()
            return null
        }
        val newest = scheduled.maxBy { it.scheduledAt }
        scheduled.filter { it.id != newest.id }.forEach { record ->
            val pendingIntent = buildAlarmPendingIntent(record.id, record.requestCode, record.alarmInstanceId)
            alarmManager.cancel(pendingIntent)
            alarmRepository.markCanceled(record.id, AlarmCancelReason.REBOOT_CLEANUP)
        }
        updateActiveAlarmIdentity(newest)
        return newest
    }

    private suspend fun scheduleAlarm(
        screenOffTs: Long,
        confirmedAt: Long,
        triggerAt: Long,
        durationUsedMinutes: Int,
        source: AlarmSource
    ): AlarmRecord {
        val scheduledAt = System.currentTimeMillis()
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
            source = source,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            snoozedAt = null,
            snoozedUntil = null,
            overlayUsed = false,
            activityPresented = false
        )
        val alarmId = alarmRepository.insertRecord(record)

        if (!PermissionUtils.canScheduleExactAlarms(context)) {
            Log.e("AlarmScheduler", "Exact alarm permission missing; cannot schedule exact alarm.")
            notificationHelper.showExactAlarmWarning()
        }

        val operation = buildAlarmPendingIntent(alarmId, requestCode, instanceId)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        updateActiveAlarmIdentity(record.copy(id = alarmId))

        val scheduledText = context.getString(
            R.string.alarm_scheduled_body,
            TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(triggerAt))
        )
        notificationHelper.showAlarmScheduled(
            scheduledText,
            com.sleep8.util.AlarmIntents.alarmDetailPendingIntent(context, alarmId.toInt(), alarmId)
        )

        return record.copy(id = alarmId)
    }

    private suspend fun cancelScheduledAlarms(reason: AlarmCancelReason) {
        val scheduled = alarmRepository.getScheduledRecords()
        scheduled.forEach { record ->
            val pendingIntent = buildAlarmPendingIntent(record.id, record.requestCode, record.alarmInstanceId)
            alarmManager.cancel(pendingIntent)
            alarmRepository.markCanceled(record.id, reason)
        }

        val activeId = appPreferences.activeAlarmId
        if (activeId > 0 && scheduled.none { it.id == activeId }) {
            cancelFromStoredIdentity()
        }
        clearActiveAlarmIdentity()
    }

    private fun cancelFromStoredIdentity() {
        val requestCode = appPreferences.activeAlarmRequestCode
        val instanceId = appPreferences.activeAlarmInstanceId
        if (requestCode <= 0 || instanceId <= 0) return
        val pendingIntent = buildAlarmPendingIntent(appPreferences.activeAlarmId, requestCode, instanceId)
        alarmManager.cancel(pendingIntent)
    }

    private fun updateActiveAlarmIdentity(record: AlarmRecord) {
        appPreferences.activeAlarmId = record.id
        appPreferences.activeAlarmRequestCode = record.requestCode
        appPreferences.activeAlarmInstanceId = record.alarmInstanceId
    }

    private fun clearActiveAlarmIdentity() {
        appPreferences.activeAlarmId = -1L
        appPreferences.activeAlarmRequestCode = -1
        appPreferences.activeAlarmInstanceId = -1L
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
