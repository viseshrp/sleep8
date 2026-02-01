package com.sleep8.domain.model

data class ArmSession(
    val id: Long,
    val armedAt: Long,
    val disarmedAt: Long?,
    val windowStartTs: Long,
    val windowEndTs: Long,
    val source: ArmSource
)
