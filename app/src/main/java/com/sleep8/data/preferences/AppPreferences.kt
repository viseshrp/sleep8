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

    fun clearPendingConfirmation() {
        pendingCandidateScreenOffTs = -1L
        pendingConfirmDeadlineTs = -1L
    }
}
