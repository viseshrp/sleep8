package com.sleep8.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarm_records",
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
data class AlarmRecordEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "alarm_id") val alarmId: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "screen_off_ts") val screenOffTs: Long,
    @ColumnInfo(name = "confirmed_at") val confirmedAt: Long,
    @ColumnInfo(name = "scheduled_alarm_ts") val scheduledAlarmTs: Long,
    @ColumnInfo(name = "os_alarm_intent_resolved") val osAlarmIntentResolved: Boolean,
    @ColumnInfo(name = "os_alarm_ui_required") val osAlarmUiRequired: Boolean?,
    @ColumnInfo(name = "internal_backstop_scheduled") val internalBackstopScheduled: Boolean
)
