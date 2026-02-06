package com.sleep8.ui.settings

import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.model.Settings
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.ui.theme.AppThemeMode
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
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
    fun `invalid duration shows error and does not persist`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        val viewModel = SettingsViewModel(settingsRepository, prefs)
        advanceUntilIdle()

        viewModel.updateAlarmDurationHours("13")
        viewModel.updateAlarmDurationMinutes("0")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.alarmDurationError)
        assertEquals("12", viewModel.uiState.value.alarmDurationHoursInput)
        assertEquals("0", viewModel.uiState.value.alarmDurationMinutesInput)
        coVerify(atLeast = 1) {
            settingsRepository.updateSettings(match { it.alarmDurationMinutes == 720 })
        }
        assertEquals(720, prefs.alarmDurationMinutes)
    }

    @Test
    fun `valid duration clears error`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        val viewModel = SettingsViewModel(settingsRepository, prefs)
        advanceUntilIdle()

        viewModel.updateAlarmDurationHours("0")
        viewModel.updateAlarmDurationMinutes("0")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.alarmDurationError)
        assertEquals("0", viewModel.uiState.value.alarmDurationHoursInput)
        assertEquals("0", viewModel.uiState.value.alarmDurationMinutesInput)
    }

    @Test
    fun `minutes normalize into hours`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        val viewModel = SettingsViewModel(settingsRepository, prefs)
        advanceUntilIdle()

        viewModel.updateAlarmDurationHours("1")
        viewModel.updateAlarmDurationMinutes("75")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.alarmDurationError)
        assertEquals("2", viewModel.uiState.value.alarmDurationHoursInput)
        assertEquals("15", viewModel.uiState.value.alarmDurationMinutesInput)
    }

    @Test
    fun `dark mode toggle updates persisted theme preference`() = runTest {
        coEvery { settingsRepository.getSettings() } returns Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 480,
            overlayEnabled = false,
            armedDefault = false
        )
        val viewModel = SettingsViewModel(settingsRepository, prefs)
        advanceUntilIdle()

        viewModel.updateDarkModeEnabled(false)

        assertEquals(AppThemeMode.LIGHT, prefs.themeMode)
        assertEquals(false, viewModel.uiState.value.darkModeEnabled)
    }
}
