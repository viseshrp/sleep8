package com.sleep8.testutil

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule

fun AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>.setResumedContent(
    content: @Composable () -> Unit
) {
    activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    runOnUiThread {
        activity.setContent(content = content)
    }
    waitForIdle()
}
