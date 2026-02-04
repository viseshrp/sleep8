package com.sleep8.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.model.Settings
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.testutil.TestActivity
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun settingsScreenShowsReliabilityChecklist() {
        val settingsRepository = mockk<SettingsRepository>()
        val armManager = mockk<ArmManager>(relaxed = true)
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "21:00",
            nightEnd = "04:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = false
        )
        val viewModel = SettingsViewModel(settingsRepository, AppPreferences(InMemorySharedPreferences()), armManager)

        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(viewModel = viewModel, onBack = { })
            }
        }

        composeRule.onNodeWithText("Night Window").assertExists()
        composeRule.onNodeWithText("Auto-arm Schedule").assertExists()
        composeRule.onNodeWithText("Alarm Behavior").assertExists()
        composeRule.onNodeWithText("System Reliability").assertExists()

        composeRule.onNodeWithText("Exact alarms").assertExists()
        composeRule.onNodeWithText("Notifications").assertExists()
        composeRule.onNodeWithText("Battery optimization").assertExists()
        composeRule.onNodeWithText("Draw over other apps (optional)").assertExists()
    }
}
