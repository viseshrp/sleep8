package com.sleep8.data.preferences

import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.ui.theme.AppThemeMode
import com.sleep8.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesTest {

    @Test
    fun `next alarm instance id increases when time is constant`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        val first = prefs.nextAlarmInstanceId()
        val second = prefs.nextAlarmInstanceId()
        assertTrue(second > first)
    }

    @Test
    fun `alarm duration migrates from legacy hours`() {
        val shared = InMemorySharedPreferences()
        shared.edit().putInt("pref_alarm_offset_hours", 7).apply()

        val prefs = AppPreferences(shared)
        val minutes = prefs.alarmDurationMinutes

        assertEquals(7 * 60, minutes)
        assertEquals(7 * 60, shared.getInt(Constants.PREF_ALARM_DURATION_MINUTES, -1))
    }

    @Test
    fun `theme mode defaults to dark and persists updates`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        assertEquals(AppThemeMode.DARK, prefs.themeMode)

        prefs.themeMode = AppThemeMode.LIGHT

        assertEquals(AppThemeMode.LIGHT, prefs.themeMode)
    }

    @Test
    fun `alarm duration uses default when no legacy value exists`() {
        val shared = InMemorySharedPreferences()
        val prefs = AppPreferences(shared)

        val minutes = prefs.alarmDurationMinutes

        assertEquals(Constants.ALARM_DEFAULT_DURATION_MINUTES, minutes)
        assertEquals(Constants.ALARM_DEFAULT_DURATION_MINUTES, shared.getInt(Constants.PREF_ALARM_DURATION_MINUTES, -1))
    }

    @Test
    fun `clear pending confirmation resets both timestamps`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.pendingCandidateScreenOffTs = 10L
        prefs.pendingConfirmDeadlineTs = 20L

        prefs.clearPendingConfirmation()

        assertEquals(-1L, prefs.pendingCandidateScreenOffTs)
        assertEquals(-1L, prefs.pendingConfirmDeadlineTs)
    }

    @Test
    fun `active alarm identity fields persist`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.activeAlarmId = 101L
        prefs.activeAlarmRequestCode = 202
        prefs.activeAlarmInstanceId = 303L

        assertEquals(101L, prefs.activeAlarmId)
        assertEquals(202, prefs.activeAlarmRequestCode)
        assertEquals(303L, prefs.activeAlarmInstanceId)
    }
}
