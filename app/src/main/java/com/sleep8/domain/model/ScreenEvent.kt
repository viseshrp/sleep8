package com.sleep8.domain.model

data class ScreenEvent(
    val id: Long,
    val sessionId: Long,
    val type: ScreenEventType,
    val ts: Long
)

enum class ScreenEventType {
    SCREEN_OFF,
    SCREEN_ON
}
