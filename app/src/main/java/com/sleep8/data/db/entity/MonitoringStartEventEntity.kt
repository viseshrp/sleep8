package com.sleep8.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitoring_start_events")
data class MonitoringStartEventEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "expected_boundary_ts") val expectedBoundaryTs: Long,
    @ColumnInfo(name = "scheduled_at_ts") val scheduledAtTs: Long,
    @ColumnInfo(name = "boundary_observed_at_ts") val boundaryObservedAtTs: Long?,
    @ColumnInfo(name = "armed_at_boundary") val armedAtBoundary: Boolean,
    @ColumnInfo(name = "in_night_window_at_boundary") val inNightWindowAtBoundary: Boolean,
    @ColumnInfo(name = "gate_open") val gateOpen: Boolean,
    @ColumnInfo(name = "boundary_trigger_executed") val boundaryTriggerExecuted: Boolean,
    @ColumnInfo(name = "monitoring_active") val monitoringActive: Boolean,
    @ColumnInfo(name = "monitoring_activated_at_ts") val monitoringActivatedAtTs: Long?,
    @ColumnInfo(name = "reason_bucket") val reasonBucket: String,
    @ColumnInfo(name = "trigger_source") val triggerSource: String,
    @ColumnInfo(name = "created_at_ts") val createdAtTs: Long
)
