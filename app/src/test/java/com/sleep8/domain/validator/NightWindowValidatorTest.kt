package com.sleep8.domain.validator

import com.sleep8.data.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class NightWindowValidatorTest {

    private fun validatorFor(start: String, end: String): NightWindowValidator {
        val dao = FakeSettingsDao(start, end)
        return NightWindowValidator(SettingsRepository(dao))
    }

    @Test
    fun `same day window - time within bounds returns true`() = runTest {
        val validator = validatorFor("09:00", "17:00")
        assertTrue(validator.isInWindow(LocalTime.of(12, 0)))
    }

    @Test
    fun `same day window - time before start returns false`() = runTest {
        val validator = validatorFor("09:00", "17:00")
        assertFalse(validator.isInWindow(LocalTime.of(8, 30)))
    }

    @Test
    fun `same day window - time after end returns false`() = runTest {
        val validator = validatorFor("09:00", "17:00")
        assertFalse(validator.isInWindow(LocalTime.of(17, 30)))
    }

    @Test
    fun `midnight crossing window - time after start returns true`() = runTest {
        val validator = validatorFor("22:00", "08:00")
        assertTrue(validator.isInWindow(LocalTime.of(23, 0)))
    }

    @Test
    fun `midnight crossing window - time before end returns true`() = runTest {
        val validator = validatorFor("22:00", "08:00")
        assertTrue(validator.isInWindow(LocalTime.of(6, 0)))
    }

    @Test
    fun `midnight crossing window - time between end and start returns false`() = runTest {
        val validator = validatorFor("22:00", "08:00")
        assertFalse(validator.isInWindow(LocalTime.of(12, 0)))
    }

    @Test
    fun `exactly at start time returns true`() = runTest {
        val validator = validatorFor("22:00", "08:00")
        assertTrue(validator.isInWindow(LocalTime.of(22, 0)))
    }

    @Test
    fun `exactly at end time returns true`() = runTest {
        val validator = validatorFor("22:00", "08:00")
        assertTrue(validator.isInWindow(LocalTime.of(8, 0)))
    }

    @Test
    fun `midnight exactly - in midnight crossing window returns true`() = runTest {
        val validator = validatorFor("22:00", "08:00")
        assertTrue(validator.isInWindow(LocalTime.of(0, 0)))
    }
}

private class FakeSettingsDao(private val start: String, private val end: String) : com.sleep8.data.db.dao.SettingsDao {
    override fun observeSettings() = kotlinx.coroutines.flow.flowOf(null)
    override suspend fun getSettings() = com.sleep8.data.db.entity.SettingsEntity(
        nightStart = start,
        nightEnd = end,
        confirmOffMinutes = 10,
        snoozeMinutes = null,
        armedDefault = false,
        offlineOnly = true
    )
    override suspend fun upsert(settings: com.sleep8.data.db.entity.SettingsEntity) = Unit
}
