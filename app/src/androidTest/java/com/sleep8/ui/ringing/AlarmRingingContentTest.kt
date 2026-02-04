package com.sleep8.ui.ringing

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.sleep8.testutil.TestActivity
import org.junit.Rule
import org.junit.Test

class AlarmRingingContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun ringingUiShowsDismissOnly() {
        composeRule.setContent {
            MaterialTheme {
                AlarmRingingContent(label = "Alarm", alarmInfo = "Alarm #123", onDismiss = {})
            }
        }

        composeRule.onNodeWithText("Dismiss").assertExists()
        composeRule.onNodeWithText("Alarm #123").assertExists()
        composeRule.onNodeWithText("Snooze").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Open menu").assertDoesNotExist()
    }
}
