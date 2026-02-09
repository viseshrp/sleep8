package com.sleep8.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.sleep8.testutil.setResumedContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDurationFieldsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun durationInputsExposeHoursAndMinutes() {
        composeRule.setResumedContent {
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
