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
        coEvery { alarmRepository.getAllRecordsNewestFirst() } returns listOf(record)
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

    private fun baseRecord(): AlarmRecord {
        return AlarmRecord(
            id = 1L,
            sessionId = 1L,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 3000L,
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
