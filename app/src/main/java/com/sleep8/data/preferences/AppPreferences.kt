package com.sleep8.data.preferences

import android.content.SharedPreferences
import com.sleep8.util.Constants

/**
 * Simple SharedPreferences wrapper for runtime state persistence.
 */
class AppPreferences(private val prefs: SharedPreferences) {

    var armed: Boolean
        get() = prefs.getBoolean(Constants.PREF_ARMED, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_ARMED, value).apply()

    var activeSessionId: Long
        get() = prefs.getLong(Constants.PREF_ACTIVE_SESSION_ID, -1L)
        set(value) = prefs.edit().putLong(Constants.PREF_ACTIVE_SESSION_ID, value).apply()

    var pendingCandidateScreenOffTs: Long
        get() = prefs.getLong(Constants.PREF_PENDING_SCREEN_OFF_TS, -1L)
        set(value) = prefs.edit().putLong(Constants.PREF_PENDING_SCREEN_OFF_TS, value).apply()

    var pendingConfirmDeadlineTs: Long
        get() = prefs.getLong(Constants.PREF_PENDING_CONFIRM_DEADLINE_TS, -1L)
        set(value) = prefs.edit().putLong(Constants.PREF_PENDING_CONFIRM_DEADLINE_TS, value).apply()

    var lastScreenOffTs: Long
        get() = prefs.getLong(Constants.PREF_LAST_SCREEN_OFF_TS, -1L)
        set(value) = prefs.edit().putLong(Constants.PREF_LAST_SCREEN_OFF_TS, value).apply()

    var clockUiWarningShown: Boolean
        get() = prefs.getBoolean(Constants.PREF_CLOCK_UI_WARNING_SHOWN, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_CLOCK_UI_WARNING_SHOWN, value).apply()

    var batteryOptOutAck: Boolean
        get() = prefs.getBoolean(Constants.PREF_BATTERY_OPT_ACK, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_BATTERY_OPT_ACK, value).apply()

    var alarmDurationMinutes: Int
        get() {
            val current = prefs.getInt(Constants.PREF_ALARM_DURATION_MINUTES, -1)
            if (current >= 0) return current
            val legacyHours = prefs.getInt(LEGACY_ALARM_OFFSET_HOURS, -1)
            val migrated = if (legacyHours > 0) legacyHours * 60 else Constants.ALARM_DEFAULT_DURATION_MINUTES
            prefs.edit().putInt(Constants.PREF_ALARM_DURATION_MINUTES, migrated).apply()
            return migrated
        }
        set(value) = prefs.edit().putInt(Constants.PREF_ALARM_DURATION_MINUTES, value).apply()

    var notificationsAsked: Boolean
        get() = prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ASKED, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_NOTIFICATIONS_ASKED, value).apply()

    private var lastAlarmInstanceId: Long
        get() = prefs.getLong(Constants.PREF_LAST_ALARM_INSTANCE_ID, 0L)
        set(value) = prefs.edit().putLong(Constants.PREF_LAST_ALARM_INSTANCE_ID, value).apply()

    fun clearPendingConfirmation() {
        pendingCandidateScreenOffTs = -1L
        pendingConfirmDeadlineTs = -1L
    }

    fun nextAlarmInstanceId(): Long {
        val now = System.currentTimeMillis()
        val next = if (now <= lastAlarmInstanceId) lastAlarmInstanceId + 1 else now
        lastAlarmInstanceId = next
        return next
    }

    private companion object {
        const val LEGACY_ALARM_OFFSET_HOURS = "pref_alarm_offset_hours"
    }
}
