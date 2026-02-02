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
    val canceledReason: AlarmCancelReason?,
    val firedAt: Long?,
    val dismissedAt: Long?,
    val overlayUsed: Boolean,
    val activityPresented: Boolean
)

enum class AlarmStatus {
    SCHEDULED,
    FIRED,
    DISMISSED,
    CANCELED
}

enum class AlarmSource {
    SLEEP_AUTOMATION
}

enum class AlarmCancelReason {
    REPLACED_BY_NEW_ALARM,
    USER_DISARM,
    USER_TOGGLE_OFF,
    REBOOT_CLEANUP
}
