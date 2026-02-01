package com.sleep8.integration

import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ServiceScenario
import com.sleep8.app.Sleep8Application
import com.sleep8.service.NightMonitorService
import com.sleep8.util.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Sleep8Application::class, sdk = [31])
class ServiceIntegrationTest {

    @Test
    fun `service starts in foreground when armed`() {
        val scenario = ServiceScenario.launch(NightMonitorService::class.java)
        scenario.onService { service ->
            service.handleArm()

            val nm = shadowOf(service.getSystemService(NotificationManager::class.java))
            assertTrue(nm.notificationChannels.any { it.id == Constants.NOTIFICATION_CHANNEL_ID })
        }
    }

    @Test
    fun `service registers screen receiver on start`() {
        val scenario = ServiceScenario.launch(NightMonitorService::class.java)
        scenario.onService { service ->
            service.handleArm()

            val shadowApp = shadowOf(service.application)
            val receivers = shadowApp.registeredReceivers
            assertTrue(receivers.any { it.intentFilter.hasAction(Intent.ACTION_SCREEN_OFF) })
        }
    }

    @Test
    fun `service unregisters receiver on stop`() {
        val scenario = ServiceScenario.launch(NightMonitorService::class.java)
        scenario.onService { service ->
            service.handleArm()
            service.handleDisarm()

            val shadowApp = shadowOf(service.application)
            val receivers = shadowApp.registeredReceivers
            assertFalse(receivers.any { it.intentFilter.hasAction(Intent.ACTION_SCREEN_OFF) })
        }
    }
}
