package com.sleep8.service.receiver

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.sleep8.domain.manager.StateMachineManager
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ConfirmationAlarmReceiverTest {

    @Test
    fun `confirmation receiver passes screen-off state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIsInteractive(false)

        val manager = mockk<StateMachineManager>(relaxed = true)
        val receiver = ConfirmationAlarmReceiver().apply {
            this.stateMachineManager = manager
            this.dispatcher = Dispatchers.Default
        }

        runBlocking { receiver.handleConfirmation(context) }

        coVerify(timeout = 1000) { manager.onConfirmationTimerExpired(true) }
    }
}
