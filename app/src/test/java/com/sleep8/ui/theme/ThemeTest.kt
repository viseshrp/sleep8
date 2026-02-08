package com.sleep8.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @After
    fun tearDown() {
        unmockkStatic(AppCompatDelegate::class)
    }

    @Test
    fun `fromPref maps known values and falls back to default`() {
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromPref("dark"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromPref("light"))
        assertEquals(AppThemeMode.DEFAULT, AppThemeMode.fromPref("unknown"))
        assertEquals(AppThemeMode.DEFAULT, AppThemeMode.fromPref(null))
    }

    @Test
    fun `apply updates appcompat night mode`() {
        mockkStatic(AppCompatDelegate::class)

        ThemeController.apply(AppThemeMode.DARK)
        ThemeController.apply(AppThemeMode.LIGHT)

        verify(exactly = 1) { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
        verify(exactly = 1) { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
    }
}
