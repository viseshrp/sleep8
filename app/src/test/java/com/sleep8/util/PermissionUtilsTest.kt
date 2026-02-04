package com.sleep8.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
}
