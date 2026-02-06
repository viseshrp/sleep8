package com.sleep8.ui.theme

import androidx.appcompat.app.AppCompatDelegate

enum class AppThemeMode(val prefValue: String, val nightMode: Int) {
    DARK(prefValue = "dark", nightMode = AppCompatDelegate.MODE_NIGHT_YES),
    LIGHT(prefValue = "light", nightMode = AppCompatDelegate.MODE_NIGHT_NO);

    companion object {
        val DEFAULT = DARK

        fun fromPref(value: String?): AppThemeMode {
            return entries.firstOrNull { it.prefValue == value } ?: DEFAULT
        }
    }
}
