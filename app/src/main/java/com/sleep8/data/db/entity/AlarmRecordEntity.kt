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
    @ColumnInfo(name = "scheduled_at") val scheduledAt: Long,
    @ColumnInfo(name = "trigger_at") val triggerAt: Long,
    @ColumnInfo(name = "duration_used_minutes") val durationUsedMinutes: Int,
    @ColumnInfo(name = "alarm_instance_id") val alarmInstanceId: Long,
    @ColumnInfo(name = "request_code") val requestCode: Int,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "canceled_reason") val canceledReason: String?,
    @ColumnInfo(name = "fired_at") val firedAt: Long?,
    @ColumnInfo(name = "dismissed_at") val dismissedAt: Long?,
    @ColumnInfo(name = "overlay_used") val overlayUsed: Boolean,
    @ColumnInfo(name = "activity_presented") val activityPresented: Boolean
)
