package com.sleep8.domain.model

enum class MonitoringReasonBucket(val label: String) {
    NONE(""),
    BOUNDARY_EVENT_DID_NOT_RUN("boundary event did not run"),
    PROCESS_NOT_STARTED("process not started"),
    START_ATTEMPT_BLOCKED("start attempt blocked"),
    APP_RESTRICTED_OR_FORCE_STOPPED_SUSPECTED("app restricted / force-stopped suspected"),
    UNKNOWN("unknown");

    companion object {
        fun fromLabel(label: String): MonitoringReasonBucket {
            return entries.firstOrNull { it.label == label } ?: UNKNOWN
        }
    }
}
