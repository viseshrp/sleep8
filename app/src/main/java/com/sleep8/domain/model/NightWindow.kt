package com.sleep8.domain.model

/**
 * Represents the night window start/end in epoch millis.
 */
data class NightWindow(
    val startTs: Long,
    val endTs: Long
)
