package com.sleep8.service.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.ui.main.MainActivity
import com.sleep8.ui.settings.SettingsActivity
import com.sleep8.util.Constants
import com.sleep8.util.PermissionUtils
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `build notification creates base channel and ongoing notification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)

        val notification = helper.buildNotification("Monitoring active")

        val nm = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNotNull(nm.notificationChannels.firstOrNull { it.id == Constants.NOTIFICATION_CHANNEL_ID })
        assertEquals("Monitoring active", notification.extras.getString("android.text"))
        val contentIntent = requireNotNull(notification.contentIntent)
        assertEquals(
            MainActivity::class.java.name,
            shadowOf(contentIntent).savedIntent.component?.className
        )
        assertTrue(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test
    fun `show warning posts notification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)

        helper.showWarning("Warning text")

        val nm = shadowOf(context.getSystemService(NotificationManager::class.java))
        val warning = requireNotNull(nm.getNotification(Constants.NOTIFICATION_ID + 1))
        assertEquals("Warning text", warning.extras.getString("android.text"))
        val contentIntent = requireNotNull(warning.contentIntent)
        assertEquals(
            MainActivity::class.java.name,
            shadowOf(contentIntent).savedIntent.component?.className
        )
    }

    @Test
    fun `show exact alarm warning posts actionable notification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)

        helper.showExactAlarmWarning()

        val nm = shadowOf(context.getSystemService(NotificationManager::class.java))
        val warning = requireNotNull(nm.getNotification(Constants.NOTIFICATION_ID + 2))
        val contentIntent = requireNotNull(warning.contentIntent)
        assertEquals(
            SettingsActivity::class.java.name,
            shadowOf(contentIntent).savedIntent.component?.className
        )
        assertTrue((warning.actions?.size ?: 0) > 0)
        val actionIntent = requireNotNull(warning.actions[0].actionIntent)
        assertEquals(
            PermissionUtils.exactAlarmIntent(context).action,
            shadowOf(actionIntent).savedIntent.action
        )
    }

    @Test
    fun `show alarm scheduled posts scheduled notification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(SettingsActivityPlaceholder::class.java.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        helper.showAlarmScheduled("Alarm at 7:00 AM", pendingIntent)

        val nm = shadowOf(context.getSystemService(NotificationManager::class.java))
        val posted = requireNotNull(nm.getNotification(Constants.ALARM_SCHEDULED_NOTIFICATION_ID))
        assertEquals("Alarm at 7:00 AM", posted.extras.getString("android.text"))
        val contentIntent = requireNotNull(posted.contentIntent)
        assertEquals(
            SettingsActivityPlaceholder::class.java.name,
            shadowOf(contentIntent).savedIntent.action
        )
    }

    @Test
    fun `clear all pending notifications clears known app notifications`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            9,
            Intent(SettingsActivityPlaceholder::class.java.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        helper.showWarning("Warning text")
        helper.showExactAlarmWarning()
        helper.showAlarmScheduled("Alarm at 7:00 AM", pendingIntent)

        helper.clearAllPendingNotifications()

        val nm = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNull(nm.getNotification(Constants.NOTIFICATION_ID))
        assertNull(nm.getNotification(Constants.NOTIFICATION_ID + 1))
        assertNull(nm.getNotification(Constants.NOTIFICATION_ID + 2))
        assertNull(nm.getNotification(Constants.ALARM_SCHEDULED_NOTIFICATION_ID))
    }

    private object SettingsActivityPlaceholder
}
