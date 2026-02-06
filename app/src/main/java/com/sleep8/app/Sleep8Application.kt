package com.sleep8.app

import android.app.Application
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.domain.manager.ArmManager
import com.sleep8.ui.theme.ThemeController
import com.sleep8.util.TimeUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point for Sleep8.
 */
@HiltAndroidApp
class Sleep8Application : Application() {

    @Inject lateinit var armManager: ArmManager
    @Inject lateinit var appPreferences: AppPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ThemeController.apply(appPreferences.themeMode)
        TimeUtils.initDefaultZone()
        appScope.launch {
            armManager.handleAutoArm()
        }
    }
}
