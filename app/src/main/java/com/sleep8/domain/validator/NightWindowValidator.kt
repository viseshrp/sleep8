package com.sleep8.domain.validator

import com.sleep8.data.repository.SettingsRepository
import com.sleep8.util.TimeUtils
import java.time.LocalTime

class NightWindowValidator(private val settingsRepository: SettingsRepository) {

    suspend fun isInWindow(now: LocalTime): Boolean {
        val settings = settingsRepository.getSettings()
        val start = TimeUtils.parseLocalTime(settings.nightStart)
        val end = TimeUtils.parseLocalTime(settings.nightEnd)
        return TimeUtils.isInWindow(now, start, end)
    }
}
