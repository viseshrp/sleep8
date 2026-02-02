package com.sleep8.domain.model

data class AlarmRecord(
    val id: Long,
    val sessionId: Long,
    val screenOffTs: Long,
    val confirmedAt: Long,
    val scheduledAt: Long,
    val triggerAt: Long,
    val durationUsedMinutes: Int,
    val alarmInstanceId: Long,
    val requestCode: Int,
    val source: AlarmSource,
    val status: AlarmStatus,
    val firedAt: Long?,
    val dismissedAt: Long?,
    val snoozedAt: Long?,
    val snoozedUntil: Long?,
    val overlayUsed: Boolean,
    val activityPresented: Boolean
)

enum class AlarmStatus {
    SCHEDULED,
    FIRED,
    DISMISSED,
    SNOOZED
}

enum class AlarmSource {
    SLEEP_AUTOMATION,
    SNOOZE
}
