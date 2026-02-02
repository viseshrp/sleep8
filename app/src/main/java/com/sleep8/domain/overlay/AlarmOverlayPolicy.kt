package com.sleep8.domain.overlay

object AlarmOverlayPolicy {

    fun shouldShowOverlay(enabled: Boolean, permissionGranted: Boolean): Boolean {
        return enabled && permissionGranted
    }

    fun shouldPromptForPermission(enabled: Boolean, permissionGranted: Boolean): Boolean {
        return enabled && !permissionGranted
    }
}
