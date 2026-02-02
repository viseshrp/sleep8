package com.sleep8.ui.ringing

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AlarmRingingContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ringingUiShowsDismissOnly() {
        composeRule.setContent {
            MaterialTheme {
                AlarmRingingContent(label = "Alarm", onDismiss = {})
            }
        }

        composeRule.onNodeWithText("Dismiss").assertExists()
        composeRule.onNodeWithText("Snooze").assertDoesNotExist()
    }
}
