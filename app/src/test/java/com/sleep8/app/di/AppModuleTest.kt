package com.sleep8.app.di

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.dao.ArmSessionDao
import com.sleep8.data.db.dao.MonitoringStartEventDao
import com.sleep8.data.db.dao.ScreenEventDao
import com.sleep8.data.db.dao.SettingsDao
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.MonitoringReliabilityRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.MonitoringHealthScheduler
import com.sleep8.domain.scheduler.NightWindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.MonitoringRuntimeInspector
import com.sleep8.service.ServiceControllerImpl
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.util.Constants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppModuleTest {

    @Test
    @Config(sdk = [31])
    fun `provideSharedPreferences uses device-protected storage when user is locked`() {
        val context = mockk<Context>()
        val lockedContext = mockk<Context>()
        val userManager = mockk<UserManager>()
        val prefs = mockk<SharedPreferences>()

        every { context.getSystemService(UserManager::class.java) } returns userManager
        every { userManager.isUserUnlocked } returns false
        every { context.createDeviceProtectedStorageContext() } returns lockedContext
        every { lockedContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs

        val actual = AppModule.provideSharedPreferences(context)

        assertSame(prefs, actual)
        verify(exactly = 1) { context.createDeviceProtectedStorageContext() }
        verify(exactly = 1) { lockedContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }
    }

    @Test
    @Config(sdk = [31])
    fun `provideSharedPreferences uses app context when unlocked`() {
        val context = mockk<Context>()
        val userManager = mockk<UserManager>()
        val prefs = mockk<SharedPreferences>()

        every { context.getSystemService(UserManager::class.java) } returns userManager
        every { userManager.isUserUnlocked } returns true
        every { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs

        val actual = AppModule.provideSharedPreferences(context)

        assertSame(prefs, actual)
        verify(exactly = 0) { context.createDeviceProtectedStorageContext() }
        verify(exactly = 1) { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }
    }

    @Test
    @Config(sdk = [31])
    fun `provideSharedPreferences falls back when user manager is null`() {
        val context = mockk<Context>()
        val prefs = mockk<SharedPreferences>()

        every { context.getSystemService(UserManager::class.java) } returns null
        every { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs

        val actual = AppModule.provideSharedPreferences(context)

        assertSame(prefs, actual)
        verify(exactly = 1) { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }
    }

    @Test
    @Config(sdk = [31])
    fun `providers return expected object types`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = mockk<AlarmManager>()
        val sharedPreferences = com.sleep8.testutil.InMemorySharedPreferences()
        val settingsDao = mockk<SettingsDao>()
        val armSessionDao = mockk<ArmSessionDao>()
        val screenEventDao = mockk<ScreenEventDao>()
        val alarmRecordDao = mockk<AlarmRecordDao>()
        val monitoringStartEventDao = mockk<MonitoringStartEventDao>()

        val providedAlarmManagerContext = mockk<Context>()
        every { providedAlarmManagerContext.getSystemService(Context.ALARM_SERVICE) } returns alarmManager

        val providedAlarmManager = AppModule.provideAlarmManager(providedAlarmManagerContext)
        val appPreferences = AppModule.provideAppPreferences(sharedPreferences)
        val settingsRepository = AppModule.provideSettingsRepository(settingsDao)
        val sessionRepository = AppModule.provideSessionRepository(armSessionDao, screenEventDao)
        val alarmRepository = AppModule.provideAlarmRepository(alarmRecordDao)
        val reliabilityRepository = AppModule.provideMonitoringReliabilityRepository(monitoringStartEventDao)
        val serviceController = AppModule.provideServiceController(appContext)
        val runtimeInspector = AppModule.provideMonitoringRuntimeInspector()
        val notificationHelper = AppModule.provideNotificationHelper(appContext)
        val stateHolder = AppModule.provideStateHolder(appPreferences)
        val confirmOffScheduler = AppModule.provideConfirmOffScheduler(appContext, alarmManager, appPreferences)
        val nightWindowScheduler = AppModule.provideNightWindowScheduler(appContext, alarmManager)
        val healthScheduler = AppModule.provideMonitoringHealthScheduler(appContext, alarmManager)
        val reliabilityManager = AppModule.provideMonitoringReliabilityManager(
            settingsRepository,
            stateHolder,
            serviceController,
            runtimeInspector,
            healthScheduler,
            appPreferences,
            reliabilityRepository
        )
        val armManager = AppModule.provideArmManager(
            sessionRepository,
            stateHolder,
            serviceController,
            settingsRepository,
            nightWindowScheduler,
            confirmOffScheduler,
            reliabilityManager
        )
        val alarmScheduler = AppModule.provideAlarmScheduler(
            appContext,
            alarmManager,
            alarmRepository,
            settingsRepository,
            appPreferences,
            notificationHelper
        )
        val stateMachineManager = AppModule.provideStateMachineManager(
            stateHolder,
            settingsRepository,
            sessionRepository,
            alarmRepository,
            confirmOffScheduler,
            alarmScheduler
        )

        assertSame(alarmManager, providedAlarmManager)
        assertNotNull(appPreferences as AppPreferences)
        assertNotNull(settingsRepository as SettingsRepository)
        assertNotNull(sessionRepository as SessionRepository)
        assertNotNull(alarmRepository as AlarmRepository)
        assertNotNull(reliabilityRepository as MonitoringReliabilityRepository)
        assertNotNull(serviceController as ServiceControllerImpl)
        assertNotNull(runtimeInspector as MonitoringRuntimeInspector)
        assertNotNull(notificationHelper as NotificationHelper)
        assertNotNull(stateHolder as StateHolder)
        assertNotNull(confirmOffScheduler as ConfirmOffScheduler)
        assertNotNull(nightWindowScheduler as NightWindowScheduler)
        assertNotNull(healthScheduler as MonitoringHealthScheduler)
        assertNotNull(reliabilityManager as MonitoringReliabilityManager)
        assertNotNull(armManager as ArmManager)
        assertNotNull(alarmScheduler as AlarmScheduler)
        assertNotNull(stateMachineManager as StateMachineManager)
    }
}
