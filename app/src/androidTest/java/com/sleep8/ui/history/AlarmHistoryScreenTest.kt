package com.sleep8.ui.history

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.testutil.setResumedContent
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmHistoryScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

        composeRule.setResumedContent {
            MaterialTheme {
                AlarmHistoryScreen(viewModel = viewModel, onBack = { backPressed = true })
            }
        }

        composeRule.runOnIdle { viewModel.loadAlarm(record.id) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Alarm History").assertExists()
        composeRule.onNodeWithText("Alarm Detail").assertExists()
        composeRule.onNodeWithText("Clear").performClick()
        composeRule.onNodeWithText("Clear alarm history?").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(backPressed) }
    }
}
