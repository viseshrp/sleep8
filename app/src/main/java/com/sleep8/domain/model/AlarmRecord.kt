package com.sleep8.domain.model

data class AlarmRecord(
    val id: Long,
    val sessionId: Long,
    val screenOffTs: Long,
    val confirmedAt: Long,
    val scheduledAlarmTs: Long,
    val osAlarmIntentResolved: Boolean,
    val osAlarmUiRequired: Boolean?,
    val internalBackstopScheduled: Boolean
)
