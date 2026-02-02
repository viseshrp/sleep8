package com.sleep8.data.repository

import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.entity.AlarmRecordEntity
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus

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

    suspend fun getAllRecordsNewestFirst(): List<AlarmRecord> {
        return alarmRecordDao.getAllRecordsNewestFirst().map { it.toDomain() }
    }

    suspend fun getLatestRecord(): AlarmRecord? {
        return alarmRecordDao.getLatestRecord()?.toDomain()
    }

    suspend fun getLatestScheduledRecord(): AlarmRecord? {
        return alarmRecordDao.getLatestByStatus(AlarmStatus.SCHEDULED.name)?.toDomain()
    }

    suspend fun markFired(alarmId: Long, firedAt: Long) {
        alarmRecordDao.markFired(alarmId, AlarmStatus.FIRED.name, firedAt)
    }

    suspend fun markDismissed(alarmId: Long, dismissedAt: Long) {
        alarmRecordDao.markDismissed(alarmId, AlarmStatus.DISMISSED.name, dismissedAt)
    }

    suspend fun markSnoozed(alarmId: Long, snoozedUntil: Long) {
        alarmRecordDao.markSnoozed(alarmId, AlarmStatus.SNOOZED.name, snoozedUntil)
    }
}

private fun AlarmRecord.toEntity(): AlarmRecordEntity {
    return AlarmRecordEntity(
        alarmId = id,
        sessionId = sessionId,
        screenOffTs = screenOffTs,
        confirmedAt = confirmedAt,
        scheduledAt = scheduledAt,
        triggerAt = triggerAt,
        durationUsedMinutes = durationUsedMinutes,
        alarmInstanceId = alarmInstanceId,
        requestCode = requestCode,
        scheduledViaAlarmClock = scheduledViaAlarmClock,
        source = source.name,
        status = status.name,
        firedAt = firedAt,
        dismissedAt = dismissedAt,
        snoozedUntil = snoozedUntil
    )
}

private fun AlarmRecordEntity.toDomain(): AlarmRecord {
    return AlarmRecord(
        id = alarmId,
        sessionId = sessionId,
        screenOffTs = screenOffTs,
        confirmedAt = confirmedAt,
        scheduledAt = scheduledAt,
        triggerAt = triggerAt,
        durationUsedMinutes = durationUsedMinutes,
        alarmInstanceId = alarmInstanceId,
        requestCode = requestCode,
        scheduledViaAlarmClock = scheduledViaAlarmClock,
        source = AlarmSource.valueOf(source),
        status = AlarmStatus.valueOf(status),
        firedAt = firedAt,
        dismissedAt = dismissedAt,
        snoozedUntil = snoozedUntil
    )
}
