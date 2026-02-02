package com.sleep8.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "night_start") val nightStart: String = "22:00",
    @ColumnInfo(name = "night_end") val nightEnd: String = "08:00",
    @ColumnInfo(name = "confirm_off_minutes") val confirmOffMinutes: Int = 10,
    @ColumnInfo(name = "snooze_minutes") val snoozeMinutes: Int? = null,
    @ColumnInfo(name = "alarm_duration_minutes") val alarmDurationMinutes: Int = 8 * 60,
    @ColumnInfo(name = "overlay_enabled") val overlayEnabled: Boolean = false,
    @ColumnInfo(name = "armed_default") val armedDefault: Boolean = false,
    @ColumnInfo(name = "auto_arm_enabled") val autoArmEnabled: Boolean = false,
    @ColumnInfo(name = "auto_arm_start") val autoArmStart: String = "22:00",
    @ColumnInfo(name = "auto_arm_end") val autoArmEnd: String = "08:00"
)
