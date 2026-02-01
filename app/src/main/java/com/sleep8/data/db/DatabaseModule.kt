package com.sleep8.data.db

import android.content.Context
import androidx.room.Room
import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.dao.ArmSessionDao
import com.sleep8.data.db.dao.ScreenEventDao
import com.sleep8.data.db.dao.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Sleep8Database {
        return Room.databaseBuilder(context, Sleep8Database::class.java, "sleep8.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSettingsDao(db: Sleep8Database): SettingsDao = db.settingsDao()

    @Provides
    fun provideArmSessionDao(db: Sleep8Database): ArmSessionDao = db.armSessionDao()

    @Provides
    fun provideScreenEventDao(db: Sleep8Database): ScreenEventDao = db.screenEventDao()

    @Provides
    fun provideAlarmRecordDao(db: Sleep8Database): AlarmRecordDao = db.alarmRecordDao()
}
