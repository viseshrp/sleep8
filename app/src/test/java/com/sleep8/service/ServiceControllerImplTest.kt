package com.sleep8.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ServiceControllerImplTest {

    @Test
    fun `start night monitor service starts foreground service`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = ServiceControllerImpl(context)

        controller.startNightMonitorService()

        val shadowApp = shadowOf(context as Application)
        val started = shadowApp.nextStartedService
        assertEquals(NightMonitorService::class.java.name, started?.component?.className)
    }

    @Test
    fun `stop night monitor service stops service`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = ServiceControllerImpl(context)

        controller.stopNightMonitorService()

        val shadowApp = shadowOf(context as Application)
        val stopped = shadowApp.nextStoppedService
        assertEquals(NightMonitorService::class.java.name, stopped?.component?.className)
    }
}
