package com.sleep8.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PermissionUtilsTest {

    @Test
    @Config(sdk = [31])
    fun `post notifications not required on pre-33`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(PermissionUtils.needsPostNotifications(context))
    }

    @Test
    @Config(sdk = [33])
    fun `post notifications required when not granted`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(PermissionUtils.needsPostNotifications(context))
    }

    @Test
    @Config(sdk = [31])
    fun `exact alarm intent uses request action on android 12 plus`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = PermissionUtils.exactAlarmIntent(context)
        org.junit.Assert.assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, intent.action)
    }

    @Test
    @Config(sdk = [33])
    fun `battery optimization intent targets app package`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = PermissionUtils.batteryOptimizationIntent(context)
        org.junit.Assert.assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertTrue(intent.dataString?.contains(context.packageName) == true)
    }

    @Test
    @Config(sdk = [33])
    fun `overlay intent targets app package`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = PermissionUtils.overlayIntent(context)
        org.junit.Assert.assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
        assertTrue(intent.dataString?.contains(context.packageName) == true)
    }

    @Test
    @Config(sdk = [34])
    fun `fullscreen intent settings action used on android 14 plus`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = PermissionUtils.fullScreenIntentSettingsIntent(context)
        org.junit.Assert.assertEquals(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, intent.action)
        assertNotNull(intent.data)
    }

    @Test
    @Config(sdk = [33])
    fun `fullscreen capability defaults to true below android 14`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(PermissionUtils.canUseFullScreenIntent(context))
        org.junit.Assert.assertEquals(Build.VERSION_CODES.TIRAMISU, Build.VERSION.SDK_INT)
    }
}
