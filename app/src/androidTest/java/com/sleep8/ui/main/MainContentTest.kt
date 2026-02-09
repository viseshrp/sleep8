package com.sleep8.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sleep8.ui.alarm.AlarmListItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationDrawerItemsTriggerCallbacks() {
        var historyOpened = false
        var settingsOpened = false

        composeRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = MainUiState(statusText = "Armed"),
                    alarmItems = emptyList(),
                    updatingAlarmIds = emptySet(),
                    onOpenSettings = { settingsOpened = true },
                    onOpenHistory = { historyOpened = true },
                    onToggleAlarm = { _, _ -> },
                    onToggleArmed = { }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open menu").performClick()
        composeRule.onNodeWithText("Alarm").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Open menu").performClick()
        composeRule.onNodeWithText("Alarm History").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(historyOpened) }

        composeRule.onNodeWithContentDescription("Open menu").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(settingsOpened) }
    }

    @Test
    fun homeAlarmListShowsTimeAndToggle() {
        var toggleCall: Pair<Long, Boolean>? = null

        composeRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = MainUiState(statusText = "Armed"),
                    alarmItems = listOf(
                        AlarmListItem(
                            id = 1L,
                            timeText = "10:15",
                            subtitle = "Scheduled from screen-off",
                            enabled = true,
                            toggleEnabled = true
                        )
                    ),
                    updatingAlarmIds = emptySet(),
                    onOpenSettings = {},
                    onOpenHistory = {},
                    onToggleAlarm = { id, enabled -> toggleCall = id to enabled },
                    onToggleArmed = {}
                )
            }
        }

        composeRule.onNodeWithText("Alarm list").assertExists()
        composeRule.onNodeWithText("10:15").assertExists()
        composeRule.onNodeWithTag("alarm-toggle-1").performClick()
        composeRule.runOnIdle { assertEquals(1L to false, toggleCall) }
    }
}
