package com.sleep8.data.repository

import com.sleep8.data.db.dao.ArmSessionDao
import com.sleep8.data.db.dao.ScreenEventDao
import com.sleep8.data.db.entity.ArmSessionEntity
import com.sleep8.data.db.entity.ScreenEventEntity
import com.sleep8.domain.model.ArmSession
import com.sleep8.domain.model.ArmSource
import com.sleep8.domain.model.ScreenEventType

class SessionRepository(
    private val armSessionDao: ArmSessionDao,
    private val screenEventDao: ScreenEventDao
) {

    suspend fun createSession(
        armedAt: Long,
        windowStartTs: Long,
        windowEndTs: Long,
        source: ArmSource
    ): ArmSession {
        val entity = ArmSessionEntity(
            armedAt = armedAt,
            windowStartTs = windowStartTs,
            windowEndTs = windowEndTs,
            source = source.name
        )
        val id = armSessionDao.insert(entity)
        return entity.copy(sessionId = id).toDomain()
    }

    suspend fun createSession(source: ArmSource): ArmSession {
        val now = System.currentTimeMillis()
        return createSession(
            armedAt = now,
            windowStartTs = now,
            windowEndTs = now,
            source = source
        )
    }

    suspend fun endSession(sessionId: Long, disarmedAt: Long) {
        armSessionDao.disarm(sessionId, disarmedAt)
    }

    suspend fun getActiveSession(): ArmSession? {
        return armSessionDao.getActiveSession()?.toDomain()
    }

    suspend fun getSession(sessionId: Long): ArmSession? {
        return armSessionDao.getSession(sessionId)?.toDomain()
    }

    suspend fun insertScreenEvent(sessionId: Long, type: ScreenEventType, ts: Long) {
        val entity = ScreenEventEntity(
            sessionId = sessionId,
            type = type.name,
            ts = ts
        )
        screenEventDao.insert(entity)
    }
}

private fun ArmSessionEntity.toDomain(): ArmSession {
    return ArmSession(
        id = sessionId,
        armedAt = armedAt,
        disarmedAt = disarmedAt,
        windowStartTs = windowStartTs,
        windowEndTs = windowEndTs,
        source = ArmSource.valueOf(source)
    )
}
