package com.sleep8.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AlarmRingerTest {

    @Test
    @Config(sdk = [31])
    fun `start and stop do not crash on modern sdk`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ringer = AlarmRinger(context)

        ringer.start()
        ringer.stop()
        ringer.stop()
    }
}
