package com.sleep8.app

import android.app.Application
import com.sleep8.util.TimeUtils
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Sleep8.
 */
@HiltAndroidApp
class Sleep8Application : Application() {
    override fun onCreate() {
        super.onCreate()
        TimeUtils.initDefaultZone()
    }
}
