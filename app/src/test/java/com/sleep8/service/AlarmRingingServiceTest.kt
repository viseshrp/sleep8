package com.sleep8.service

import android.app.Service
import android.content.Intent
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.testutil.InMemorySharedPreferences
import com.sleep8.util.Constants
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AlarmRingingServiceTest {

    @Test
    fun `ring action without valid alarm id is ignored`() {
        val controller = Robolectric.buildService(AlarmRingingService::class.java).create()
        val service = controller.get()
        service.stateHolder = mockk<StateHolder>(relaxed = true)
        service.alarmRepository = mockk<AlarmRepository>(relaxed = true)
        service.settingsRepository = mockk<SettingsRepository>(relaxed = true)
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
        service.settingsRepository = mockk<SettingsRepository>(relaxed = true)
        service.appPreferences = AppPreferences(InMemorySharedPreferences())
        service.notificationHelper = NotificationHelper(service)

        val dismissIntent = Intent(service, AlarmRingingService::class.java).apply {
            action = Constants.ACTION_ALARM_DISMISS
            putExtra(Constants.EXTRA_ALARM_ID, 42L)
        }
        service.onStartCommand(dismissIntent, 0, 0)

        verify(exactly = 1) { stateHolder.clearLastScreenOffTs() }
    }
}
