package com.sleep8.app.di

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.dao.ArmSessionDao
import com.sleep8.data.db.dao.ScreenEventDao
import com.sleep8.data.db.dao.SettingsDao
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.NightWindowScheduler
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.domain.scheduler.WindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.service.ServiceControllerImpl
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(settingsDao: SettingsDao): SettingsRepository {
        return SettingsRepository(settingsDao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        armSessionDao: ArmSessionDao,
        screenEventDao: ScreenEventDao
    ): SessionRepository {
        return SessionRepository(armSessionDao, screenEventDao)
    }

    @Provides
    @Singleton
    fun provideAlarmRepository(alarmRecordDao: AlarmRecordDao): AlarmRepository {
        return AlarmRepository(alarmRecordDao)
    }

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideAppPreferences(sharedPreferences: SharedPreferences): AppPreferences {
        return AppPreferences(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideServiceController(@ApplicationContext context: Context): ServiceController {
        return ServiceControllerImpl(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideStateHolder(appPreferences: AppPreferences): StateHolder {
        return StateHolder(appPreferences)
    }

    @Provides
    @Singleton
    fun provideArmManager(
        sessionRepository: SessionRepository,
        stateHolder: StateHolder,
        serviceController: ServiceController,
        windowScheduler: WindowScheduler,
        settingsRepository: SettingsRepository,
        nightWindowScheduler: NightWindowScheduler,
        confirmOffScheduler: ConfirmOffScheduler
    ): ArmManager {
        return ArmManager(
            sessionRepository,
            stateHolder,
            serviceController,
            windowScheduler,
            settingsRepository,
            nightWindowScheduler,
            confirmOffScheduler
        )
    }

    @Provides
    @Singleton
    fun provideStateMachineManager(
        stateHolder: StateHolder,
        settingsRepository: SettingsRepository,
        sessionRepository: SessionRepository,
        alarmRepository: AlarmRepository,
        confirmOffScheduler: ConfirmOffScheduler,
        alarmScheduler: AlarmScheduler
    ): StateMachineManager {
        return StateMachineManager(
            stateHolder,
            settingsRepository,
            sessionRepository,
            alarmRepository,
            confirmOffScheduler,
            alarmScheduler
        )
    }

    @Provides
    @Singleton
    fun provideConfirmOffScheduler(
        @ApplicationContext context: Context,
        alarmManager: AlarmManager,
        appPreferences: AppPreferences
    ): ConfirmOffScheduler {
        return ConfirmOffScheduler(context, alarmManager, appPreferences)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        alarmManager: AlarmManager,
        alarmRepository: AlarmRepository,
        settingsRepository: SettingsRepository,
        appPreferences: AppPreferences,
        notificationHelper: NotificationHelper
    ): AlarmScheduler {
        return AlarmScheduler(
            context,
            alarmManager,
            alarmRepository,
            settingsRepository,
            appPreferences,
            notificationHelper
        )
    }

    @Provides
    @Singleton
    fun provideWindowScheduler(
        @ApplicationContext context: Context,
        alarmManager: AlarmManager
    ): WindowScheduler {
        return WindowScheduler(context, alarmManager)
    }

    @Provides
    @Singleton
    fun provideNightWindowScheduler(
        @ApplicationContext context: Context,
        alarmManager: AlarmManager
    ): NightWindowScheduler {
        return NightWindowScheduler(context, alarmManager)
    }
}
