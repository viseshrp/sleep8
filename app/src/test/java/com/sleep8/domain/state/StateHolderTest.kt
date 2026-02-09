package com.sleep8.domain.state

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.domain.model.AppState
import com.sleep8.testutil.InMemorySharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StateHolderTest {

    @Test
    fun `initial state reflects persisted armed flag`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.armed = true

        val holder = StateHolder(prefs)

        assertEquals(AppState.ARMED_IDLE, holder.state.value)
    }

    @Test
    fun `initial state is disarmed when persisted flag is false`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.armed = false

        val holder = StateHolder(prefs)

        assertEquals(AppState.DISARMED, holder.state.value)
    }

    @Test
    fun `last screen off timestamp is restored and persisted`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        prefs.lastScreenOffTs = 1234L
        val holder = StateHolder(prefs)
        assertEquals(1234L, holder.lastScreenOffTs.value)

        holder.setLastScreenOffTs(5678L)

        assertEquals(5678L, holder.lastScreenOffTs.value)
        assertEquals(5678L, prefs.lastScreenOffTs)
    }

    @Test
    fun `clear last screen off timestamp resets flow and prefs`() {
        val prefs = AppPreferences(InMemorySharedPreferences())
        val holder = StateHolder(prefs)
        holder.setLastScreenOffTs(5678L)

        holder.clearLastScreenOffTs()

        assertEquals(-1L, holder.lastScreenOffTs.value)
        assertEquals(-1L, prefs.lastScreenOffTs)
    }
}
