package com.sleep8.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.dao.ArmSessionDao
import com.sleep8.data.db.dao.ScreenEventDao
import com.sleep8.data.db.dao.SettingsDao
import com.sleep8.data.db.entity.AlarmRecordEntity
import com.sleep8.data.db.entity.ArmSessionEntity
import com.sleep8.data.db.entity.ScreenEventEntity
import com.sleep8.data.db.entity.SettingsEntity

@Database(
    entities = [
        SettingsEntity::class,
        ArmSessionEntity::class,
        ScreenEventEntity::class,
        AlarmRecordEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class Sleep8Database : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun armSessionDao(): ArmSessionDao
    abstract fun screenEventDao(): ScreenEventDao
    abstract fun alarmRecordDao(): AlarmRecordDao
}
