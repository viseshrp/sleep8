package com.sleep8.ui.alarm

import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmCancelReason
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.domain.scheduler.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class AlarmListViewModelTest {

    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggle off cancels scheduled alarm`() = runTest {
        val record = baseRecord(status = AlarmStatus.SCHEDULED)
        coEvery { alarmRepository.getAllRecordsNewestFirst() } returns listOf(record)
        coEvery { alarmRepository.getRecord(record.id) } returns record

        val viewModel = AlarmListViewModel(alarmRepository, alarmScheduler)
        advanceUntilIdle()

        viewModel.onToggle(record.id, enabled = false)
        advanceUntilIdle()

        coVerify { alarmScheduler.cancelAlarm(record, AlarmCancelReason.USER_TOGGLE_OFF) }
    }

    @Test
    fun `toggle on enables disabled alarm`() = runTest {
        val record = baseRecord(status = AlarmStatus.CANCELED, cancelReason = AlarmCancelReason.USER_TOGGLE_OFF)
        coEvery { alarmRepository.getAllRecordsNewestFirst() } returns listOf(record)
        coEvery { alarmRepository.getRecord(record.id) } returns record

        val viewModel = AlarmListViewModel(alarmRepository, alarmScheduler)
        advanceUntilIdle()

        viewModel.onToggle(record.id, enabled = true)
        advanceUntilIdle()

        coVerify { alarmScheduler.enableExisting(record) }
    }

    @Test
    fun `past alarms are not toggleable`() = runTest {
        val pastRecord = baseRecord(status = AlarmStatus.SCHEDULED, triggerAt = System.currentTimeMillis() - 10_000L)
        coEvery { alarmRepository.getAllRecordsNewestFirst() } returns listOf(pastRecord)

        val viewModel = AlarmListViewModel(alarmRepository, alarmScheduler)
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.first()
        assertFalse(item.toggleEnabled)
        assertTrue(item.enabled)
    }

    @Test
    fun `refresh keeps only the most recent alarm`() = runTest {
        val mostRecent = baseRecord(status = AlarmStatus.SCHEDULED, triggerAt = System.currentTimeMillis() + 60_000L)
        val older = baseRecord(
            status = AlarmStatus.CANCELED,
            cancelReason = AlarmCancelReason.USER_TOGGLE_OFF,
            triggerAt = System.currentTimeMillis() + 120_000L
        ).copy(id = 2L)
        coEvery { alarmRepository.getAllRecordsNewestFirst() } returns listOf(mostRecent, older)

        val viewModel = AlarmListViewModel(alarmRepository, alarmScheduler)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(mostRecent.id, viewModel.uiState.value.items.first().id)
    }

    private fun baseRecord(
        status: AlarmStatus,
        cancelReason: AlarmCancelReason? = null,
        triggerAt: Long = System.currentTimeMillis() + 60_000L
    ): AlarmRecord {
        return AlarmRecord(
            id = 1L,
            sessionId = 1L,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 3000L,
            triggerAt = triggerAt,
            durationUsedMinutes = 480,
            alarmInstanceId = 100L,
            requestCode = 100,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = status,
            canceledReason = cancelReason,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
    }
}
