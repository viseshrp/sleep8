package com.sleep8.ui.theme

import androidx.appcompat.app.AppCompatDelegate

object ThemeController {
    fun apply(mode: AppThemeMode) {
        AppCompatDelegate.setDefaultNightMode(mode.nightMode)
    }
}
