package com.sleep8.ui.alarm

data class AlarmListUiState(
    val items: List<AlarmListItem> = emptyList(),
    val updatingIds: Set<Long> = emptySet()
)
