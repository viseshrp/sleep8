package com.sleep8.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sleep8.util.Constants

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "night_start") val nightStart: String = Constants.DEFAULT_NIGHT_START,
    @ColumnInfo(name = "night_end") val nightEnd: String = Constants.DEFAULT_NIGHT_END,
    @ColumnInfo(name = "confirm_off_minutes") val confirmOffMinutes: Int = Constants.DEFAULT_CONFIRM_MINUTES,
    @ColumnInfo(name = "alarm_duration_minutes") val alarmDurationMinutes: Int = Constants.ALARM_DEFAULT_DURATION_MINUTES,
    @ColumnInfo(name = "overlay_enabled") val overlayEnabled: Boolean = false,
    @ColumnInfo(name = "armed_default") val armedDefault: Boolean = false
)
