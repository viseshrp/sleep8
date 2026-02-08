package com.sleep8.data.repository

import com.sleep8.data.db.dao.MonitoringStartEventDao
import com.sleep8.data.db.entity.MonitoringStartEventEntity
import com.sleep8.domain.model.MonitoringReasonBucket
import com.sleep8.domain.model.MonitoringTriggerSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringReliabilityRepositoryTest {

    private val dao = mockk<MonitoringStartEventDao>(relaxed = true)
    private val repository = MonitoringReliabilityRepository(dao)

    @Test
    fun `recordBoundaryScheduled persists scheduled-only event`() = runBlocking {
        val captured = slot<MonitoringStartEventEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L

        repository.recordBoundaryScheduled(expectedBoundaryTs = 1_000L, scheduledAtTs = 900L)

        coVerify(exactly = 1) { dao.insert(any()) }
        assertEquals(1_000L, captured.captured.expectedBoundaryTs)
        assertEquals(900L, captured.captured.scheduledAtTs)
        assertEquals(MonitoringTriggerSource.SCHEDULE.value, captured.captured.triggerSource)
        assertEquals(MonitoringReasonBucket.NONE.label, captured.captured.reasonBucket)
        assertTrue(captured.captured.boundaryObservedAtTs == null)
        assertTrue(captured.captured.monitoringActivatedAtTs == null)
    }

    @Test
    fun `recordTrigger persists full trigger telemetry`() = runBlocking {
        val captured = slot<MonitoringStartEventEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L

        repository.recordTrigger(
            expectedBoundaryTs = 10_000L,
            scheduledAtTs = 9_000L,
            observedAtTs = 10_100L,
            armedAtBoundary = true,
            inNightWindowAtBoundary = true,
            gateOpen = true,
            boundaryTriggerExecuted = true,
            monitoringActive = true,
            monitoringActivatedAtTs = 10_200L,
            reasonBucket = MonitoringReasonBucket.NONE,
            triggerSource = MonitoringTriggerSource.PERIODIC_HEALTH_CHECK
        )

        coVerify(exactly = 1) { dao.insert(any()) }
        assertEquals(10_000L, captured.captured.expectedBoundaryTs)
        assertEquals(9_000L, captured.captured.scheduledAtTs)
        assertEquals(10_100L, captured.captured.boundaryObservedAtTs)
        assertEquals(10_100L, captured.captured.createdAtTs)
        assertEquals(10_200L, captured.captured.monitoringActivatedAtTs)
        assertEquals(MonitoringTriggerSource.PERIODIC_HEALTH_CHECK.value, captured.captured.triggerSource)
    }

    @Test
    fun `latest delegates to dao`() = runBlocking {
        val entity = MonitoringStartEventEntity(
            expectedBoundaryTs = 1L,
            scheduledAtTs = 2L,
            boundaryObservedAtTs = 3L,
            armedAtBoundary = true,
            inNightWindowAtBoundary = true,
            gateOpen = true,
            boundaryTriggerExecuted = true,
            monitoringActive = true,
            monitoringActivatedAtTs = 4L,
            reasonBucket = MonitoringReasonBucket.NONE.label,
            triggerSource = MonitoringTriggerSource.SCHEDULE.value,
            createdAtTs = 5L
        )
        coEvery { dao.latest() } returns entity

        val latest = repository.latest()

        assertEquals(entity, latest)
        coVerify(exactly = 1) { dao.latest() }
    }

    @Test
    fun `latestScheduledBoundary uses schedule source`() = runBlocking {
        coEvery { dao.latestBySource(MonitoringTriggerSource.SCHEDULE.value) } returns null

        repository.latestScheduledBoundary()

        coVerify(exactly = 1) { dao.latestBySource(MonitoringTriggerSource.SCHEDULE.value) }
    }

    @Test
    fun `hasBoundaryExecution uses boundary trigger source`() = runBlocking {
        coEvery { dao.hasBoundaryExecution(123L, MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM.value) } returns true

        val result = repository.hasBoundaryExecution(123L)

        assertTrue(result)
        coVerify(exactly = 1) {
            dao.hasBoundaryExecution(123L, MonitoringTriggerSource.NIGHT_WINDOW_BOUNDARY_ALARM.value)
        }
    }

    @Test
    fun `latestForBoundary delegates expected boundary`() = runBlocking {
        coEvery { dao.latestForBoundary(456L) } returns null

        repository.latestForBoundary(456L)

        coVerify(exactly = 1) { dao.latestForBoundary(456L) }
    }
}
