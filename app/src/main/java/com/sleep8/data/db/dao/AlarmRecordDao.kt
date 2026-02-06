package com.sleep8.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sleep8.data.db.entity.AlarmRecordEntity

@Dao
interface AlarmRecordDao {

    @Insert
    suspend fun insert(record: AlarmRecordEntity): Long

    @Query("SELECT * FROM alarm_records WHERE session_id = :sessionId ORDER BY confirmed_at ASC")
    suspend fun getRecordsForSession(sessionId: Long): List<AlarmRecordEntity>

    @Query("SELECT * FROM alarm_records WHERE alarm_id = :alarmId")
    suspend fun getRecord(alarmId: Long): AlarmRecordEntity?

    @Query("SELECT * FROM alarm_records ORDER BY scheduled_at DESC")
    suspend fun getAllRecordsNewestFirst(): List<AlarmRecordEntity>

    @Query("SELECT * FROM alarm_records ORDER BY scheduled_at DESC LIMIT 1")
    suspend fun getLatestRecord(): AlarmRecordEntity?

    @Query("SELECT * FROM alarm_records WHERE status = :status ORDER BY trigger_at DESC LIMIT 1")
    suspend fun getLatestByStatus(status: String): AlarmRecordEntity?

    @Query("SELECT * FROM alarm_records WHERE status = :status ORDER BY scheduled_at DESC")
    suspend fun getRecordsByStatus(status: String): List<AlarmRecordEntity>

    @Query("DELETE FROM alarm_records")
    suspend fun deleteAll()

    @Query("UPDATE alarm_records SET status = :status, fired_at = :firedAt WHERE alarm_id = :alarmId")
    suspend fun markFired(alarmId: Long, status: String, firedAt: Long)

    @Query("UPDATE alarm_records SET status = :status, dismissed_at = :dismissedAt WHERE alarm_id = :alarmId")
    suspend fun markDismissed(alarmId: Long, status: String, dismissedAt: Long)

    @Query("UPDATE alarm_records SET status = :status, canceled_reason = :reason WHERE alarm_id = :alarmId")
    suspend fun markCanceled(alarmId: Long, status: String, reason: String?)

    @Query(
        "UPDATE alarm_records SET status = :status, canceled_reason = NULL, scheduled_at = :scheduledAt, " +
            "alarm_instance_id = :alarmInstanceId, request_code = :requestCode WHERE alarm_id = :alarmId"
    )
    suspend fun markScheduled(
        alarmId: Long,
        status: String,
        scheduledAt: Long,
        alarmInstanceId: Long,
        requestCode: Int
    )

    @Query("UPDATE alarm_records SET overlay_used = :overlayUsed WHERE alarm_id = :alarmId")
    suspend fun markOverlayUsed(alarmId: Long, overlayUsed: Boolean)

    @Query("UPDATE alarm_records SET activity_presented = :activityPresented WHERE alarm_id = :alarmId")
    suspend fun markActivityPresented(alarmId: Long, activityPresented: Boolean)
}
