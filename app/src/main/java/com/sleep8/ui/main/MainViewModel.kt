package com.sleep8.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.state.StateHolder
import com.sleep8.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val armManager: ArmManager,
    private val stateHolder: StateHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var lastAutoArmSyncMs: Long = 0L

    init {
        viewModelScope.launch {
            while (true) {
                syncAutoArmIfNeeded()
                updateState()
                delay(1000)
            }
        }
    }

    private suspend fun syncAutoArmIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastAutoArmSyncMs < 60_000L) return
        lastAutoArmSyncMs = now
        armManager.syncAutoArmStateNow()
    }

    private fun updateState() {
        val state = stateHolder.state.value
        val armed = state != AppState.DISARMED
        val lastScreenOffTs = stateHolder.lastScreenOffTs.value
        val pendingDeadline = stateHolder.pendingConfirmDeadlineTs.value
        val now = System.currentTimeMillis()
        val pendingRemaining = if (pendingDeadline > 0) pendingDeadline - now else 0L

        val armedUntilText = stateHolder.activeSession.value?.windowEndTs?.takeIf { it > 0 }?.let {
            TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it))
        }.orEmpty()

        val pendingText = if (pendingRemaining > 0) {
            TimeUtils.formatCountdown(Duration.ofMillis(pendingRemaining))
        } else {
            ""
        }

        val lastScreenOffText = if (lastScreenOffTs > 0) {
            val time = TimeUtils.toLocalTime(lastScreenOffTs)
            TimeUtils.formatAlarmTime(time)
        } else {
            ""
        }

        val statusText = when (state) {
            AppState.DISARMED -> "Disarmed"
            AppState.ARMED_IDLE -> "Armed"
            AppState.ARMED_PENDING_CONFIRM -> "Confirming screen off"
            AppState.ARMED_ALARM_SET -> "Alarm created"
        }

        _uiState.value = MainUiState(
            armed = armed,
            statusText = statusText,
            armedUntilText = armedUntilText,
            lastScreenOffText = lastScreenOffText,
            pendingCountdownText = pendingText,
            showPending = pendingRemaining > 0
        )
    }

    fun toggleArmed() {
        viewModelScope.launch {
            if (armManager.isArmed()) {
                armManager.disarm()
            } else {
                armManager.arm(com.sleep8.domain.model.ArmSource.APP_BUTTON)
            }
            updateState()
        }
    }
}
