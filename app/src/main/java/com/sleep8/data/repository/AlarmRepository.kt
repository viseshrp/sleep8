package com.sleep8.data.repository

import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.entity.AlarmRecordEntity
import com.sleep8.domain.model.AlarmRecord

class AlarmRepository(private val alarmRecordDao: AlarmRecordDao) {

    suspend fun insertRecord(record: AlarmRecord): Long {
        return alarmRecordDao.insert(record.toEntity())
    }

    suspend fun getAlarmsForSession(sessionId: Long): List<AlarmRecord> {
        return alarmRecordDao.getRecordsForSession(sessionId).map { it.toDomain() }
    }

    suspend fun getRecord(alarmId: Long): AlarmRecord? {
        return alarmRecordDao.getRecord(alarmId)?.toDomain()
    }
}

private fun AlarmRecord.toEntity(): AlarmRecordEntity {
    return AlarmRecordEntity(
        alarmId = id,
        sessionId = sessionId,
        screenOffTs = screenOffTs,
        confirmedAt = confirmedAt,
        scheduledAlarmTs = scheduledAlarmTs,
        osAlarmIntentResolved = osAlarmIntentResolved,
        osAlarmUiRequired = osAlarmUiRequired,
        internalBackstopScheduled = internalBackstopScheduled
    )
}

private fun AlarmRecordEntity.toDomain(): AlarmRecord {
    return AlarmRecord(
        id = alarmId,
        sessionId = sessionId,
        screenOffTs = screenOffTs,
        confirmedAt = confirmedAt,
        scheduledAlarmTs = scheduledAlarmTs,
        osAlarmIntentResolved = osAlarmIntentResolved,
        osAlarmUiRequired = osAlarmUiRequired,
        internalBackstopScheduled = internalBackstopScheduled
    )
}
