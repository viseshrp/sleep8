package com.sleep8.ui.alarm

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AlarmListContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun alarmListShowsTimeAndToggle() {
        composeRule.setContent {
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
}
