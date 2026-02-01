package com.sleep8.service.receiver

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.data.repository.SessionRepository
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.Settings
import com.sleep8.domain.scheduler.ConfirmOffScheduler
import com.sleep8.domain.scheduler.OsAlarmCreator
import com.sleep8.domain.scheduler.WindowScheduler
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.ServiceController
import com.sleep8.testutil.InMemorySharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    @Test
    fun `manual override persists across boot and auto-arm boundaries stay scheduled`() {
        val receiver = BootReceiver()
        val appPreferences = AppPreferences(InMemorySharedPreferences()).apply {
            manualOverrideActive = true
        }
        val stateHolder = StateHolder(appPreferences)

        receiver.sessionRepository = mockk<SessionRepository> {
            coEvery { getActiveSession() } returns null
        }
        receiver.settingsRepository = mockk<SettingsRepository> {
            coEvery { getSettings() } returns Settings(
                nightStart = "22:00",
                nightEnd = "08:00",
                confirmOffMinutes = 10,
                snoozeMinutes = null,
                alarmOffsetHours = 8,
                armedDefault = false,
                autoArmEnabled = true,
                autoArmStart = "22:00",
                autoArmEnd = "08:00"
            )
        }
        receiver.stateHolder = stateHolder
        receiver.serviceController = mockk(relaxed = true)
        receiver.confirmOffScheduler = mockk(relaxed = true)
        receiver.osAlarmCreator = mockk(relaxed = true)
        receiver.windowScheduler = mockk(relaxed = true)
        receiver.stateMachineManager = mockk(relaxed = true)
        receiver.armManager = mockk(relaxed = true)
        receiver.appPreferences = appPreferences

        val context = ApplicationProvider.getApplicationContext<Context>()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        // Allow the boot coroutine to execute.
        TimeUnit.MILLISECONDS.sleep(50)

        coVerify { receiver.windowScheduler.scheduleWindowStart(any()) }
        coVerify { receiver.windowScheduler.scheduleWindowEnd(any()) }
        coVerify(exactly = 0) { receiver.armManager.arm(any()) }
        assertTrue(appPreferences.manualOverrideActive)
    }
}
