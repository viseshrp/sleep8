package com.sleep8.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sleep8.data.db.entity.MonitoringStartEventEntity

@Dao
interface MonitoringStartEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MonitoringStartEventEntity): Long

    @Query("SELECT * FROM monitoring_start_events ORDER BY created_at_ts DESC LIMIT 1")
    suspend fun latest(): MonitoringStartEventEntity?

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM monitoring_start_events " +
            "WHERE expected_boundary_ts = :expectedBoundaryTs " +
            "AND boundary_trigger_executed = 1 " +
            "AND trigger_source = :triggerSource" +
        ")"
    )
    suspend fun hasBoundaryExecution(expectedBoundaryTs: Long, triggerSource: String): Boolean

    @Query(
        "SELECT * FROM monitoring_start_events " +
            "WHERE trigger_source = :triggerSource " +
            "ORDER BY created_at_ts DESC LIMIT 1"
    )
    suspend fun latestBySource(triggerSource: String): MonitoringStartEventEntity?

    @Query(
        "SELECT * FROM monitoring_start_events " +
            "WHERE expected_boundary_ts = :expectedBoundaryTs " +
            "ORDER BY created_at_ts DESC LIMIT 1"
    )
    suspend fun latestForBoundary(expectedBoundaryTs: Long): MonitoringStartEventEntity?
}
