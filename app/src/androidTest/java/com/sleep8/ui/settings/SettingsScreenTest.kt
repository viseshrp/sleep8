package com.sleep8.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.Settings
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.mockk
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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

        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(viewModel = viewModel, onBack = { })
            }
        }

        composeRule.onNodeWithText("Night Window").assertExists()
        composeRule.onNodeWithText("Appearance").assertExists()
        composeRule.onNodeWithText("Dark mode").assertExists()
        composeRule.onNodeWithText("Alarm Behavior").performScrollTo().assertExists()
        composeRule.onNodeWithText("System Reliability").performScrollTo().assertExists()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Exact alarms", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Exact alarms", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Notifications", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Battery optimization", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Draw over other apps (optional)", useUnmergedTree = true).assertExists()
    }
}
