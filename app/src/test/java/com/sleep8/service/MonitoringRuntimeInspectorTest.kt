package com.sleep8.service

import com.sleep8.util.PermissionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringRuntimeInspectorTest {

    @After
    fun tearDown() {
        unmockkObject(PermissionUtils)
    }

    @Test
    fun `is monitoring active delegates to PermissionUtils`() {
        val context = mockk<android.content.Context>(relaxed = true)
        mockkObject(PermissionUtils)
        every { PermissionUtils.isServiceRunning(context, NightMonitorService::class.java) } returns true

        val inspector = MonitoringRuntimeInspector()
        val result = inspector.isMonitoringActive(context)

        assertTrue(result)
        verify(exactly = 1) { PermissionUtils.isServiceRunning(context, NightMonitorService::class.java) }
    }

    @Test
    fun `is monitoring active returns false when service is not running`() {
        val context = mockk<android.content.Context>(relaxed = true)
        mockkObject(PermissionUtils)
        every { PermissionUtils.isServiceRunning(context, NightMonitorService::class.java) } returns false

        val inspector = MonitoringRuntimeInspector()

        assertFalse(inspector.isMonitoringActive(context))
    }
}
