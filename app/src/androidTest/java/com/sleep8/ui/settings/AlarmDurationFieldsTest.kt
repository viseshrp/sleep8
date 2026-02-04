package com.sleep8.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.sleep8.testutil.TestActivity
import org.junit.Rule
import org.junit.Test

class AlarmDurationFieldsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun durationInputsExposeHoursAndMinutes() {
        composeRule.setContent {
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
