package com.sleep8.service.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.util.Constants
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class NotificationHelperTest {

    @Test
    fun `ensure alarm ringing channel creates channel`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)
        helper.ensureAlarmRingingChannel()

        val nm = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNotNull(nm.notificationChannels.firstOrNull { it.id == Constants.ALARM_RINGING_CHANNEL_ID })
    }

    @Test
    fun `ensure alarm scheduled channel creates channel`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)
        helper.ensureAlarmScheduledChannel()

        val nm = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNotNull(nm.notificationChannels.firstOrNull { it.id == Constants.ALARM_SCHEDULED_CHANNEL_ID })
    }
}
