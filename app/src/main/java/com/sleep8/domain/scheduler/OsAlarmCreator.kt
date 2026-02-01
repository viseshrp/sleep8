package com.sleep8.domain.scheduler

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.sleep8.R
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.util.TimeUtils
import java.time.Instant

class OsAlarmCreator(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmRepository,
    private val backstopAlarmScheduler: BackstopAlarmScheduler,
    private val appPreferences: AppPreferences,
    private val notificationHelper: NotificationHelper
) {

    suspend fun createAlarm(screenOffTs: Long): AlarmCreationResult {
        val settings = settingsRepository.getSettings()
        val screenOffInstant = Instant.ofEpochMilli(screenOffTs)
        val alarmInstant = TimeUtils.calculateAlarmTime(screenOffInstant, settings.alarmOffsetHours)
        val alarmTime = TimeUtils.toLocalTime(alarmInstant.toEpochMilli())

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, context.getString(R.string.alarm_message))
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            settings.snoozeMinutes?.let { snooze ->
                putExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, snooze)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolves = intent.resolveActivity(context.packageManager) != null
        val uiRequired: Boolean? = null

        if (resolves) {
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // If startActivity fails, treat as unresolved
            }
        } else if (!appPreferences.clockUiWarningShown) {
            appPreferences.clockUiWarningShown = true
            notificationHelper.showWarning(context.getString(R.string.clock_unavailable))
        }

        backstopAlarmScheduler.scheduleBackstop(alarmInstant.toEpochMilli())

        val record = AlarmRecord(
            id = 0,
            sessionId = appPreferences.activeSessionId,
            screenOffTs = screenOffTs,
            confirmedAt = System.currentTimeMillis(),
            scheduledAlarmTs = alarmInstant.toEpochMilli(),
            osAlarmIntentResolved = resolves,
            osAlarmUiRequired = uiRequired,
            internalBackstopScheduled = true
        )
        alarmRepository.insertRecord(record)
        return AlarmCreationResult.Success(record)
    }
}

sealed class AlarmCreationResult {
    data class Success(val record: AlarmRecord) : AlarmCreationResult()
}
