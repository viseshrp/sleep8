package com.sleep8.data.repository

import com.sleep8.data.db.dao.SettingsDao
import com.sleep8.data.db.entity.SettingsEntity
import com.sleep8.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val settingsDao: SettingsDao) {

    fun observeSettings(): Flow<Settings> {
        return settingsDao.observeSettings().map { entity ->
            entity?.toDomain() ?: SettingsEntity().toDomain()
        }
    }

    suspend fun getSettings(): Settings {
        val existing = settingsDao.getSettings()
        val entity = existing ?: SettingsEntity()
        if (existing == null) {
            settingsDao.upsert(entity)
        }
        return entity.toDomain()
    }

    suspend fun updateSettings(settings: Settings) {
        settingsDao.upsert(settings.toEntity())
    }

    suspend fun updateNightWindow(nightStart: String, nightEnd: String) {
        val current = getSettings()
        updateSettings(
            current.copy(
                nightStart = nightStart,
                nightEnd = nightEnd
            )
        )
    }
}

private fun SettingsEntity.toDomain(): Settings {
    return Settings(
        nightStart = nightStart,
        nightEnd = nightEnd,
        confirmOffMinutes = confirmOffMinutes,
        snoozeMinutes = snoozeMinutes,
        alarmOffsetHours = alarmOffsetHours,
        armedDefault = armedDefault,
        offlineOnly = offlineOnly
    )
}

private fun Settings.toEntity(): SettingsEntity {
    return SettingsEntity(
        id = 1,
        nightStart = nightStart,
        nightEnd = nightEnd,
        confirmOffMinutes = confirmOffMinutes,
        snoozeMinutes = snoozeMinutes,
        alarmOffsetHours = alarmOffsetHours,
        armedDefault = armedDefault,
        offlineOnly = offlineOnly
    )
}
