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
}
