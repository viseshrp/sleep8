package com.sleep8.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDurationFieldsTest {

    @get:Rule
    val composeRule = createComposeRule()

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
