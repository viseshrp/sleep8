package com.sleep8.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sleep8.data.db.entity.ScreenEventEntity

@Dao
interface ScreenEventDao {

    @Insert
    suspend fun insert(event: ScreenEventEntity): Long

    @Query("SELECT * FROM screen_events WHERE session_id = :sessionId ORDER BY ts ASC")
    suspend fun getEventsForSession(sessionId: Long): List<ScreenEventEntity>
}
