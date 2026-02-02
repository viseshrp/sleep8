package com.sleep8.ui.history

import com.sleep8.domain.model.AlarmRecord

data class AlarmHistoryUiState(
    val alarms: List<AlarmRecord> = emptyList(),
    val selectedAlarm: AlarmRecord? = null
)
