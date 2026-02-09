package com.sleep8.ui.alarm

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmListContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setTestContent(content: @Composable () -> Unit) {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.runOnUiThread {
            composeRule.activity.setContent(content = content)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun alarmListShowsTimeAndToggle() {
        setTestContent {
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
        setTestContent {
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
        setTestContent {
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
