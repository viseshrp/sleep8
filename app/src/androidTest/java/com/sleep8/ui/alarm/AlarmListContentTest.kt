package com.sleep8.ui.alarm

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sleep8.testutil.setResumedContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmListContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun alarmListShowsTimeAndToggle() {
        composeRule.setResumedContent {
            MaterialTheme {
                AlarmListContent(
                    items = listOf(
                        AlarmListItem(
                            id = 1L,
                            timeText = "10:15",
                            subtitle = "Scheduled from screen-off",
                            enabled = true,
                            toggleEnabled = true
                        )
                    ),
                    updatingIds = emptySet(),
                    onToggle = { _, _ -> },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("10:15").assertExists()
        composeRule.onNodeWithTag("alarm-toggle-1").assertExists()
        composeRule.onNodeWithText("Edit").assertDoesNotExist()
    }

    @Test
    fun alarmListShowsEmptyState() {
        composeRule.setResumedContent {
            MaterialTheme {
                AlarmListContent(
                    items = emptyList(),
                    updatingIds = emptySet(),
                    onToggle = { _, _ -> },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("No alarms yet.").assertExists()
    }

    @Test
    fun alarmListBackInvokesCallback() {
        var backPressed = false
        composeRule.setResumedContent {
            MaterialTheme {
                AlarmListContent(
                    items = emptyList(),
                    updatingIds = emptySet(),
                    onToggle = { _, _ -> },
                    onBack = { backPressed = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(backPressed) }
    }
}
