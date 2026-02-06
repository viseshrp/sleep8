package com.sleep8.data.repository

import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.entity.AlarmRecordEntity
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmCancelReason
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

    suspend fun getScheduledRecords(): List<AlarmRecord> {
        return alarmRecordDao.getRecordsByStatus(AlarmStatus.SCHEDULED.name).map { it.toDomain() }
    }

    suspend fun markFired(alarmId: Long, firedAt: Long) {
        alarmRecordDao.markFired(alarmId, AlarmStatus.FIRED.name, firedAt)
    }

    suspend fun markDismissed(alarmId: Long, dismissedAt: Long) {
        alarmRecordDao.markDismissed(alarmId, AlarmStatus.DISMISSED.name, dismissedAt)
    }

    suspend fun markCanceled(alarmId: Long, reason: AlarmCancelReason?) {
        alarmRecordDao.markCanceled(alarmId, AlarmStatus.CANCELED.name, reason?.name)
    }

    suspend fun markScheduled(alarmId: Long, scheduledAt: Long, alarmInstanceId: Long, requestCode: Int) {
        alarmRecordDao.markScheduled(
            alarmId = alarmId,
            status = AlarmStatus.SCHEDULED.name,
            scheduledAt = scheduledAt,
            alarmInstanceId = alarmInstanceId,
            requestCode = requestCode
        )
    }

    suspend fun markOverlayUsed(alarmId: Long) {
        alarmRecordDao.markOverlayUsed(alarmId, true)
    }

    suspend fun markActivityPresented(alarmId: Long) {
        alarmRecordDao.markActivityPresented(alarmId, true)
    }

    suspend fun clearAllRecords() {
        alarmRecordDao.deleteAll()
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
        source = source.name,
        status = status.name,
        canceledReason = canceledReason?.name,
        firedAt = firedAt,
        dismissedAt = dismissedAt,
        overlayUsed = overlayUsed,
        activityPresented = activityPresented
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
        source = AlarmSource.valueOf(source),
        status = AlarmStatus.valueOf(status),
        canceledReason = canceledReason?.let { AlarmCancelReason.valueOf(it) },
        firedAt = firedAt,
        dismissedAt = dismissedAt,
        overlayUsed = overlayUsed,
        activityPresented = activityPresented
    )
}
