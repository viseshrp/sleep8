package com.sleep8.data.repository

import com.sleep8.data.db.dao.MonitoringStartEventDao
import com.sleep8.data.db.entity.MonitoringStartEventEntity
import com.sleep8.domain.model.MonitoringReasonBucket
import com.sleep8.domain.model.MonitoringTriggerSource

class MonitoringReliabilityRepository(
    private val dao: MonitoringStartEventDao
) {

    suspend fun recordBoundaryScheduled(expectedBoundaryTs: Long, scheduledAtTs: Long) {
        dao.insert(
            MonitoringStartEventEntity(
                expectedBoundaryTs = expectedBoundaryTs,
                scheduledAtTs = scheduledAtTs,
                boundaryObservedAtTs = null,
                armedAtBoundary = false,
                inNightWindowAtBoundary = false,
                gateOpen = false,
                boundaryTriggerExecuted = false,
                monitoringActive = false,
                monitoringActivatedAtTs = null,
                reasonBucket = MonitoringReasonBucket.NONE.label,
                triggerSource = MonitoringTriggerSource.SCHEDULE.value,
                createdAtTs = scheduledAtTs
            )
        )
    }

    suspend fun recordTrigger(
        expectedBoundaryTs: Long,
        scheduledAtTs: Long,
        observedAtTs: Long,
        armedAtBoundary: Boolean,
        inNightWindowAtBoundary: Boolean,
        gateOpen: Boolean,
        boundaryTriggerExecuted: Boolean,
        monitoringActive: Boolean,
        monitoringActivatedAtTs: Long?,
        reasonBucket: MonitoringReasonBucket,
        triggerSource: MonitoringTriggerSource
    ) {
        dao.insert(
            MonitoringStartEventEntity(
                expectedBoundaryTs = expectedBoundaryTs,
                scheduledAtTs = scheduledAtTs,
                boundaryObservedAtTs = observedAtTs,
                armedAtBoundary = armedAtBoundary,
                inNightWindowAtBoundary = inNightWindowAtBoundary,
                gateOpen = gateOpen,
                boundaryTriggerExecuted = boundaryTriggerExecuted,
                monitoringActive = monitoringActive,
                monitoringActivatedAtTs = monitoringActivatedAtTs,
                reasonBucket = reasonBucket.label,
                triggerSource = triggerSource.value,
                createdAtTs = observedAtTs
            )
        )
    }

    suspend fun latest(): MonitoringStartEventEntity? = dao.latest()

    suspend fun latestScheduledBoundary(): MonitoringStartEventEntity? {
        return dao.latestBySource(MonitoringTriggerSource.SCHEDULE.value)
    }

    suspend fun hasBoundaryExecution(expectedBoundaryTs: Long): Boolean {
        return dao.hasBoundaryExecution(expectedBoundaryTs, MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM.value)
    }

    suspend fun latestForBoundary(expectedBoundaryTs: Long): MonitoringStartEventEntity? {
        return dao.latestForBoundary(expectedBoundaryTs)
    }
}
