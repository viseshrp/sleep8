package com.sleep8.ui.ringing

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.sleep8.testutil.setResumedContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmRingingContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun ringingUiShowsDismissOnly() {
        composeRule.setResumedContent {
            MaterialTheme {
                AlarmRingingContent(label = "Alarm", alarmInfo = "Alarm #123", onDismiss = {})
            }
        }

        composeRule.onNodeWithText("Dismiss").assertExists()
        composeRule.onNodeWithTag("ringing-dismiss").assertExists()
        composeRule.onNodeWithText("Alarm #123").assertExists()
        composeRule.onNodeWithContentDescription("Sleep8").assertExists()
        composeRule.onNodeWithText("Snooze").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Open menu").assertDoesNotExist()
    }
}
