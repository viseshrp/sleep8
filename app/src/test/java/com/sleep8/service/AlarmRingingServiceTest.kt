package com.sleep8.service

import android.Manifest
import android.app.Application
import android.app.Service
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.util.Constants
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AlarmRingingServiceTest {

    @Test
    fun `new alarm id stops currently ringing resources`() {
        val oldRinger = mockk<AlarmRinger>(relaxed = true)

        val controller = Robolectric.buildService(AlarmRingingService::class.java).create()
        val service = controller.get()
        service.stateHolder = mockk<StateHolder>(relaxed = true)
        service.alarmRepository = mockk<AlarmRepository>(relaxed = true)
        service.appPreferences = AppPreferences(InMemorySharedPreferences())
        service.notificationHelper = NotificationHelper(service)

        setPrivateField(service, "alarmId", 100L)
        setPrivateField(service, "ringer", oldRinger)

        val ringIntent = Intent(service, AlarmRingingService::class.java).apply {
            action = Constants.ACTION_ALARM_RING
            putExtra(Constants.EXTRA_ALARM_ID, 200L)
        }

        service.onStartCommand(ringIntent, 0, 0)

        verify(exactly = 1) { oldRinger.stop() }
        assertEquals(200L, privateLongField(service, "alarmId"))
    }

    @Test
    fun `ring action without valid alarm id is ignored`() {
        val controller = Robolectric.buildService(AlarmRingingService::class.java).create()
        val service = controller.get()
        service.stateHolder = mockk<StateHolder>(relaxed = true)
        service.alarmRepository = mockk<AlarmRepository>(relaxed = true)
        service.appPreferences = AppPreferences(InMemorySharedPreferences())
        service.notificationHelper = NotificationHelper(service)

        val ringIntent = Intent(service, AlarmRingingService::class.java).apply {
            action = Constants.ACTION_ALARM_RING
        }

        val result = service.onStartCommand(ringIntent, 0, 0)

        assertEquals(Service.START_NOT_STICKY, result)
    }

    @Test
    fun `dismiss action clears last screen off state`() {
        val stateHolder = mockk<StateHolder>(relaxed = true)
        val controller = Robolectric.buildService(AlarmRingingService::class.java).create()
        val service = controller.get()
        service.stateHolder = stateHolder
        service.alarmRepository = mockk<AlarmRepository>(relaxed = true)
        service.appPreferences = AppPreferences(InMemorySharedPreferences())
        service.notificationHelper = NotificationHelper(service)

        val dismissIntent = Intent(service, AlarmRingingService::class.java).apply {
            action = Constants.ACTION_ALARM_DISMISS
            putExtra(Constants.EXTRA_ALARM_ID, 42L)
        }
        service.onStartCommand(dismissIntent, 0, 0)

        verify(exactly = 1) { stateHolder.clearLastScreenOffTs() }
    }

    @Test
    fun `dismiss action without alarm id is ignored`() {
        val stateHolder = mockk<StateHolder>(relaxed = true)
        val controller = Robolectric.buildService(AlarmRingingService::class.java).create()
        val service = controller.get()
        service.stateHolder = stateHolder
        service.alarmRepository = mockk<AlarmRepository>(relaxed = true)
        service.appPreferences = AppPreferences(InMemorySharedPreferences())
        service.notificationHelper = NotificationHelper(service)

        val dismissIntent = Intent(service, AlarmRingingService::class.java).apply {
            action = Constants.ACTION_ALARM_DISMISS
        }
        service.onStartCommand(dismissIntent, 0, 0)

        verify(exactly = 0) { stateHolder.clearLastScreenOffTs() }
    }

    @Test
    fun `ringing while device in use does not launch activity directly`() {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(appContext)
        while (shadowApp.nextStartedActivity != null) {}
        shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val controller = Robolectric.buildService(AlarmRingingService::class.java).create()
        val service = controller.get()
        service.stateHolder = mockk<StateHolder>(relaxed = true)
        service.alarmRepository = mockk<AlarmRepository>(relaxed = true)
        service.appPreferences = AppPreferences(InMemorySharedPreferences()).apply {
            activeAlarmId = 44L
        }
        service.notificationHelper = NotificationHelper(service)
        setPrivateField(service, "ringer", mockk<AlarmRinger>(relaxed = true))

        val ringIntent = Intent(service, AlarmRingingService::class.java).apply {
            action = Constants.ACTION_ALARM_RING
            putExtra(Constants.EXTRA_ALARM_ID, 44L)
        }

        service.onStartCommand(ringIntent, 0, 0)

        assertNull(shadowApp.nextStartedActivity)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun privateLongField(target: Any, fieldName: String): Long {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.getLong(target)
    }
}
