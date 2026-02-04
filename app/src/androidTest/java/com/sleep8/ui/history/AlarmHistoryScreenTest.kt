package com.sleep8.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AlarmHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun historyScreenShowsAlarmDetailsAndBack() {
        val alarmRepository = mockk<AlarmRepository>()
        val record = AlarmRecord(
            id = 99L,
            sessionId = 1L,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 111L,
            requestCode = 111,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        coEvery { alarmRepository.getAllRecordsNewestFirst() } returns listOf(record)
        coEvery { alarmRepository.getRecord(record.id) } returns record

        val viewModel = AlarmHistoryViewModel(alarmRepository)
        var backPressed = false

        composeRule.setContent {
            MaterialTheme {
                AlarmHistoryScreen(viewModel = viewModel, onBack = { backPressed = true })
            }
        }

        composeRule.runOnIdle { viewModel.loadAlarm(record.id) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Alarm History").assertExists()
        composeRule.onNodeWithText("Alarm Detail").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(backPressed) }
    }
}
