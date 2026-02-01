package com.sleep8.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screen_events",
    foreignKeys = [
        ForeignKey(
            entity = ArmSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class ScreenEventEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "event_id") val eventId: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "ts") val ts: Long
)
