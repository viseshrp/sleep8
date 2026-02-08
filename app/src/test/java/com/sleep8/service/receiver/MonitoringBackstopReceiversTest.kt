package com.sleep8.service.receiver

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.domain.model.MonitoringTriggerSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MonitoringBackstopReceiversTest {

    @Test
    fun `monitoring health receiver triggers periodic health check`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = mockk<MonitoringReliabilityManager>(relaxed = true)
        coEvery { manager.onTrigger(any(), any()) } returns Unit
        val receiver = MonitoringHealthCheckReceiver().apply {
            monitoringReliabilityManager = manager
        }

        receiver.handleHealthCheck(context)

        coVerify(exactly = 1) {
            manager.onTrigger(context, MonitoringTriggerSource.PERIODIC_HEALTH_CHECK)
        }
    }

    @Test
    fun `night window backstop receiver triggers backstop source`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = mockk<MonitoringReliabilityManager>(relaxed = true)
        coEvery { manager.onTrigger(any(), any()) } returns Unit
        val receiver = NightWindowBackstopReceiver().apply {
            monitoringReliabilityManager = manager
        }

        receiver.handleBackstop(context)

        coVerify(exactly = 1) {
            manager.onTrigger(context, MonitoringTriggerSource.NIGHT_WINDOW_BACKSTOP)
        }
    }
}
