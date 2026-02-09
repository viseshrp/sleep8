package com.sleep8.domain.state

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.domain.model.AppState
import com.sleep8.domain.model.ArmSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory state holder backed by shared preferences for resilience.
 */
class StateHolder(private val prefs: AppPreferences) {
    companion object {
        const val NO_LAST_SCREEN_OFF_TS = -1L
    }


    private val _state = MutableStateFlow(if (prefs.armed) AppState.ARMED_IDLE else AppState.DISARMED)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _activeSession = MutableStateFlow<ArmSession?>(null)
    val activeSession: StateFlow<ArmSession?> = _activeSession.asStateFlow()

    private val _pendingCandidateScreenOffTs = MutableStateFlow(prefs.pendingCandidateScreenOffTs)
    val pendingCandidateScreenOffTs: StateFlow<Long> = _pendingCandidateScreenOffTs.asStateFlow()

    private val _pendingConfirmDeadlineTs = MutableStateFlow(prefs.pendingConfirmDeadlineTs)
    val pendingConfirmDeadlineTs: StateFlow<Long> = _pendingConfirmDeadlineTs.asStateFlow()

    private val _lastScreenOffTs = MutableStateFlow(prefs.lastScreenOffTs)
    val lastScreenOffTs: StateFlow<Long> = _lastScreenOffTs.asStateFlow()

    fun setState(state: AppState) {
        _state.value = state
    }

    fun setArmed(armed: Boolean) {
        prefs.armed = armed
        _state.value = if (armed) AppState.ARMED_IDLE else AppState.DISARMED
    }

    fun setActiveSession(session: ArmSession?) {
        _activeSession.value = session
        prefs.activeSessionId = session?.id ?: -1L
    }

    fun setPendingCandidate(screenOffTs: Long, confirmDeadlineTs: Long) {
        _pendingCandidateScreenOffTs.value = screenOffTs
        _pendingConfirmDeadlineTs.value = confirmDeadlineTs
        prefs.pendingCandidateScreenOffTs = screenOffTs
        prefs.pendingConfirmDeadlineTs = confirmDeadlineTs
    }

    fun clearPendingCandidate() {
        _pendingCandidateScreenOffTs.value = -1L
        _pendingConfirmDeadlineTs.value = -1L
        prefs.clearPendingConfirmation()
    }

    fun setLastScreenOffTs(ts: Long) {
        _lastScreenOffTs.value = ts
        prefs.lastScreenOffTs = ts
    }

    fun clearLastScreenOffTs() {
        setLastScreenOffTs(NO_LAST_SCREEN_OFF_TS)
    }
}
