package com.sleep8.util

object Constants {
    const val ALARM_OFFSET_HOURS = 8
    const val DEFAULT_CONFIRM_MINUTES = 10
    const val DEFAULT_NIGHT_START = "22:00"
    const val DEFAULT_NIGHT_END = "08:00"

    const val NOTIFICATION_CHANNEL_ID = "sleep8_monitoring"
    const val NOTIFICATION_ID = 1001

    const val ACTION_CONFIRMATION = "com.sleep8.action.CONFIRMATION"
    const val ACTION_BACKSTOP = "com.sleep8.action.BACKSTOP"
    const val ACTION_WINDOW_END = "com.sleep8.action.WINDOW_END"
    const val ACTION_WINDOW_START = "com.sleep8.action.WINDOW_START"
    const val ACTION_NIGHT_WINDOW_START = "com.sleep8.action.NIGHT_WINDOW_START"
    const val ACTION_NIGHT_WINDOW_END = "com.sleep8.action.NIGHT_WINDOW_END"

    const val PENDING_INTENT_REQUEST_CONFIRM = 2001
    const val PENDING_INTENT_REQUEST_BACKSTOP = 2002
    const val PENDING_INTENT_REQUEST_WINDOW_END = 2003
    const val PENDING_INTENT_REQUEST_WINDOW_START = 2004
    const val PENDING_INTENT_REQUEST_NIGHT_WINDOW_START = 2005
    const val PENDING_INTENT_REQUEST_NIGHT_WINDOW_END = 2006

    const val PREFS_NAME = "sleep8_prefs"
    const val PREF_ARMED = "pref_armed"
    const val PREF_ACTIVE_SESSION_ID = "pref_active_session_id"
    const val PREF_PENDING_SCREEN_OFF_TS = "pref_pending_screen_off_ts"
    const val PREF_PENDING_CONFIRM_DEADLINE_TS = "pref_pending_confirm_deadline_ts"
    const val PREF_LAST_SCREEN_OFF_TS = "pref_last_screen_off_ts"
    const val PREF_CLOCK_UI_WARNING_SHOWN = "pref_clock_ui_warning_shown"
    const val PREF_BATTERY_OPT_ACK = "pref_battery_opt_ack"
    const val PREF_ALARM_OFFSET_HOURS = "pref_alarm_offset_hours"
    const val PREF_MANUAL_OVERRIDE_ACTIVE = "pref_manual_override_active"
}
