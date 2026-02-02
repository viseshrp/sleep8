package com.sleep8.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.ui.history.AlarmHistoryActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmNotificationFactoryTest {

    private fun pendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmHistoryActivity::class.java)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Test
    fun `ringing notification includes dismiss action and alarm category`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = AlarmNotificationFactory(context)
        val notification = factory.buildRingingNotification(
            alarmIntent = pendingIntent(context, 1),
            contentIntent = pendingIntent(context, 2),
            dismissIntent = pendingIntent(context, 3)
        )

        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertNotNull(notification.actions)
        assertEquals(1, notification.actions.size)
    }
}
