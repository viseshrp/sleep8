package com.sleep8.ui.settings

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.model.Settings
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val armManager = mockk<ArmManager>(relaxed = true)
    private val prefs = AppPreferences(InMemorySharedPreferences())
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid duration shows error and is clamped on save`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = false,
            autoArmStart = "22:00",
            autoArmEnd = "08:00"
        )
        val viewModel = SettingsViewModel(settingsRepository, prefs, armManager)
        advanceUntilIdle()

        viewModel.updateAlarmDurationMinutes("721")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.alarmDurationError)
        coVerify { settingsRepository.updateSettings(match { it.alarmDurationMinutes == 720 }) }
    }

    @Test
    fun `valid duration clears error`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            snoozeMinutes = null,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false,
            autoArmEnabled = false,
            autoArmStart = "22:00",
            autoArmEnd = "08:00"
        )
        val viewModel = SettingsViewModel(settingsRepository, prefs, armManager)
        advanceUntilIdle()

        viewModel.updateAlarmDurationMinutes("0")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.alarmDurationError)
        assertEquals("0", viewModel.uiState.value.alarmDurationMinutesInput)
    }
}
