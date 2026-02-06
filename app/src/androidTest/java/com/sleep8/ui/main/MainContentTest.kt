package com.sleep8.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationDrawerItemsTriggerCallbacks() {
        var alarmOpened = false
        var historyOpened = false
        var settingsOpened = false

        composeRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = MainUiState(statusText = "Armed"),
                    onOpenSettings = { settingsOpened = true },
                    onOpenAlarm = { alarmOpened = true },
                    onOpenHistory = { historyOpened = true },
                    onToggleArmed = { }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open menu").performClick()
        composeRule.onNodeWithText("Alarm").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(alarmOpened) }

        composeRule.onNodeWithContentDescription("Open menu").performClick()
        composeRule.onNodeWithText("Alarm History").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(historyOpened) }

        composeRule.onNodeWithContentDescription("Open menu").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(settingsOpened) }
    }
}
