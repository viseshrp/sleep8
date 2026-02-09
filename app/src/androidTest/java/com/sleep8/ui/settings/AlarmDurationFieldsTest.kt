package com.sleep8.ui.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDurationFieldsTest {

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
    fun durationInputsExposeHoursAndMinutes() {
        setTestContent {
            MaterialTheme {
                AlarmDurationFields(
                    hours = "8",
                    minutes = "0",
                    error = null,
                    onHoursChanged = {},
                    onMinutesChanged = {},
                    onReset = {}
                )
            }
        }

        composeRule.onNodeWithTag("alarm-duration-hours").assertExists()
        composeRule.onNodeWithTag("alarm-duration-minutes").assertExists()
    }
}
