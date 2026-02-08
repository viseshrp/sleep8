package com.sleep8.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MainUiStateTest {

    @Test
    fun `defaults represent empty disarmed presentation`() {
        val state = MainUiState()

        assertFalse(state.armed)
        assertEquals("", state.statusText)
        assertEquals("", state.monitoringHealthText)
        assertEquals("", state.pendingCountdownText)
        assertFalse(state.showPending)
    }
}
