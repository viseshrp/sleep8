package com.sleep8.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.Settings
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.testutil.setResumedContent
import io.mockk.coEvery
import io.mockk.mockk
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreenShowsReliabilityChecklist() {
        val settingsRepository = mockk<SettingsRepository>()
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "21:00",
            nightEnd = "04:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        val viewModel = SettingsViewModel(settingsRepository, AppPreferences(InMemorySharedPreferences()))

        composeRule.setResumedContent {
            MaterialTheme {
                SettingsScreen(viewModel = viewModel, onBack = { })
            }
        }

        composeRule.onNodeWithText("Night Window").assertExists()
        composeRule.onNodeWithText("Appearance").assertExists()
        composeRule.onNodeWithText("Dark mode").assertExists()
        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("Alarm Behavior"))
        composeRule.onNodeWithText("Alarm Behavior", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("System Reliability"))
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Exact alarms", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("System Reliability", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("Exact alarms"))
        composeRule.onNodeWithText("Exact alarms", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("Notifications"))
        composeRule.onNodeWithText("Notifications", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("Battery optimization"))
        composeRule.onNodeWithText("Battery optimization", useUnmergedTree = true).assertExists()
    }
}
