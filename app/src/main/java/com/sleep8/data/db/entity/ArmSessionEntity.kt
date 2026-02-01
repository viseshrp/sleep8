package com.sleep8.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arm_sessions")
data class ArmSessionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "session_id") val sessionId: Long = 0,
    @ColumnInfo(name = "armed_at") val armedAt: Long,
    @ColumnInfo(name = "disarmed_at") val disarmedAt: Long? = null,
    @ColumnInfo(name = "window_start_ts") val windowStartTs: Long,
    @ColumnInfo(name = "window_end_ts") val windowEndTs: Long,
    @ColumnInfo(name = "source") val source: String
)
