package com.sleep8.ui.history

import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmHistoryViewModelTest {

    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
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
    fun `clear history removes alarms from ui state`() = runTest {
        val record = baseRecord()
        coEvery { alarmRepository.getRecordsNewestFirstPaged(11, 0) } returns listOf(record)
        coEvery { alarmRepository.getRecord(record.id) } returns record
        coEvery { alarmRepository.clearAllRecords() } returns Unit

        val viewModel = AlarmHistoryViewModel(alarmRepository)
        advanceUntilIdle()
        viewModel.loadAlarm(record.id)
        advanceUntilIdle()

        viewModel.clearHistory()
        advanceUntilIdle()

        assertEquals(emptyList<AlarmRecord>(), viewModel.uiState.value.alarms)
        assertNull(viewModel.uiState.value.selectedAlarm)
        coVerify(exactly = 1) { alarmRepository.clearAllRecords() }
    }

    @Test
    fun `refresh re-syncs selected alarm with latest list data`() = runTest {
        val initial = baseRecord()
        val selectedFromDetail = initial.copy(status = AlarmStatus.SCHEDULED)
        val updated = initial.copy(status = AlarmStatus.FIRED, firedAt = 4000L)
        coEvery { alarmRepository.getRecordsNewestFirstPaged(11, 0) } returnsMany listOf(
            listOf(initial),
            listOf(updated)
        )
        coEvery { alarmRepository.getRecord(initial.id) } returns selectedFromDetail

        val viewModel = AlarmHistoryViewModel(alarmRepository)
        advanceUntilIdle()
        viewModel.loadAlarm(initial.id)
        advanceUntilIdle()
        assertEquals(selectedFromDetail, viewModel.uiState.value.selectedAlarm)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(updated, viewModel.uiState.value.selectedAlarm)
        assertEquals(listOf(updated), viewModel.uiState.value.alarms)
    }

    @Test
    fun `refresh clears selected alarm when it no longer exists`() = runTest {
        val record = baseRecord()
        coEvery { alarmRepository.getRecordsNewestFirstPaged(11, 0) } returnsMany listOf(
            listOf(record),
            emptyList()
        )
        coEvery { alarmRepository.getRecord(record.id) } returnsMany listOf(record, null)

        val viewModel = AlarmHistoryViewModel(alarmRepository)
        advanceUntilIdle()
        viewModel.loadAlarm(record.id)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(emptyList<AlarmRecord>(), viewModel.uiState.value.alarms)
        assertNull(viewModel.uiState.value.selectedAlarm)
    }

    @Test
    fun `load next page appends only ten records at a time`() = runTest {
        val records = (1L..25L).map { id -> baseRecord(id = id, scheduledAt = 3_000L + id) }
        coEvery { alarmRepository.getRecordsNewestFirstPaged(11, 0) } returns records.take(11)
        coEvery { alarmRepository.getRecordsNewestFirstPaged(11, 10) } returns records.drop(10).take(11)
        coEvery { alarmRepository.getRecordsNewestFirstPaged(11, 20) } returns records.drop(20)

        val viewModel = AlarmHistoryViewModel(alarmRepository)
        advanceUntilIdle()
        assertEquals(10, viewModel.uiState.value.alarms.size)
        assertEquals(true, viewModel.uiState.value.hasMore)

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(20, viewModel.uiState.value.alarms.size)
        assertEquals(true, viewModel.uiState.value.hasMore)

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(25, viewModel.uiState.value.alarms.size)
        assertEquals(false, viewModel.uiState.value.hasMore)
    }

    private fun baseRecord(
        id: Long = 1L,
        scheduledAt: Long = 3000L
    ): AlarmRecord {
        return AlarmRecord(
            id = id,
            sessionId = 1L,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = scheduledAt,
            triggerAt = 3600L,
            durationUsedMinutes = 480,
            alarmInstanceId = 100L,
            requestCode = 100,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
    }
}
