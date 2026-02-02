package com.sleep8.domain.overlay

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlarmOverlayPolicyTest {

    @Test
    fun `should show overlay only when enabled and permission granted`() {
        assertFalse(AlarmOverlayPolicy.shouldShowOverlay(enabled = false, permissionGranted = false))
        assertFalse(AlarmOverlayPolicy.shouldShowOverlay(enabled = true, permissionGranted = false))
        assertFalse(AlarmOverlayPolicy.shouldShowOverlay(enabled = false, permissionGranted = true))
        assertTrue(AlarmOverlayPolicy.shouldShowOverlay(enabled = true, permissionGranted = true))
    }

    @Test
    fun `should prompt for permission when enabled but not granted`() {
        assertFalse(AlarmOverlayPolicy.shouldPromptForPermission(enabled = false, permissionGranted = false))
        assertFalse(AlarmOverlayPolicy.shouldPromptForPermission(enabled = false, permissionGranted = true))
        assertTrue(AlarmOverlayPolicy.shouldPromptForPermission(enabled = true, permissionGranted = false))
        assertFalse(AlarmOverlayPolicy.shouldPromptForPermission(enabled = true, permissionGranted = true))
    }
}
