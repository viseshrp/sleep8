package com.sleep8.service.receiver

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.domain.manager.ArmManager
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class WindowReceiversTest {

    @Test
    fun `window start triggers scheduled arm`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = mockk<ArmManager>(relaxed = true)
        val receiver = WindowStartReceiver().apply {
            this.armManager = manager
            this.dispatcher = Dispatchers.Default
        }

        receiver.handleWindowStart()

        verify(timeout = 1000) { manager.onScheduledEvent("start") }
    }

    @Test
    fun `window end triggers scheduled disarm`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = mockk<ArmManager>(relaxed = true)
        val receiver = WindowEndReceiver().apply {
            this.armManager = manager
            this.dispatcher = Dispatchers.Default
        }

        runBlocking { receiver.handleWindowEnd() }

        coVerify(timeout = 1000) { manager.disarm() }
    }
}
