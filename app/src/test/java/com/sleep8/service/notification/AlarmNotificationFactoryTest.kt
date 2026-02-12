package com.sleep8.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.ui.history.AlarmHistoryActivity
import com.sleep8.ui.ringing.AlarmRingingActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

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

    private fun ringingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmRingingActivity::class.java)
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
        val fullScreen = ringingIntent(context, 1)
        val content = pendingIntent(context, 2)
        val notification = factory.buildRingingNotification(
            alarmIntent = fullScreen,
            contentIntent = content,
            dismissIntent = pendingIntent(context, 3)
        )

        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertNotNull(notification.contentIntent)
        assertEquals(
            AlarmHistoryActivity::class.java.name,
            shadowOf(notification.contentIntent!!).savedIntent.component?.className
        )
        assertNotNull(notification.actions)
        assertEquals(1, notification.actions.size)
        assertEquals(fullScreen, notification.fullScreenIntent)
    }
}
