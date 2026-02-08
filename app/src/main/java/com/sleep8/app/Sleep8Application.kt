package com.sleep8.app

import android.app.Application
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.ui.theme.ThemeController
import com.sleep8.util.TimeUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point for Sleep8.
 */
@HiltAndroidApp
class Sleep8Application : Application() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        ThemeController.apply(appPreferences.themeMode)
        TimeUtils.initDefaultZone()
    }
}
