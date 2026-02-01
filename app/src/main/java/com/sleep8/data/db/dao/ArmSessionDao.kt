package com.sleep8.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sleep8.data.db.entity.ArmSessionEntity

@Dao
interface ArmSessionDao {

    @Insert
    suspend fun insert(session: ArmSessionEntity): Long

    @Query("SELECT * FROM arm_sessions WHERE disarmed_at IS NULL ORDER BY armed_at DESC LIMIT 1")
    suspend fun getActiveSession(): ArmSessionEntity?

    @Query("SELECT * FROM arm_sessions WHERE session_id = :sessionId")
    suspend fun getSession(sessionId: Long): ArmSessionEntity?

    @Query("UPDATE arm_sessions SET disarmed_at = :disarmedAt WHERE session_id = :sessionId")
    suspend fun disarm(sessionId: Long, disarmedAt: Long)

    @Query("DELETE FROM arm_sessions WHERE session_id = :sessionId")
    suspend fun delete(sessionId: Long)
}
