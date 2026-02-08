package com.sleep8.ui.ringing

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmRingingActivityTest {

    @Test
    fun `activity finishes immediately when alarm id missing`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AlarmRingingActivity::class.java)

        val activity = Robolectric.buildActivity(AlarmRingingActivity::class.java, intent).create().get()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `dismiss broadcast finishes when alarm id matches`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val intent = Intent(app, AlarmRingingActivity::class.java).apply {
            putExtra(Constants.EXTRA_ALARM_ID, 10L)
        }
        val controller = Robolectric.buildActivity(AlarmRingingActivity::class.java, intent).create()
        val activity = controller.get()
        assertFalse(activity.isFinishing)

        closeReceiver(activity).onReceive(activity, Intent(Constants.ACTION_ALARM_DISMISS).apply {
            putExtra(Constants.EXTRA_ALARM_ID, 10L)
        })

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `dismiss broadcast does not finish for different alarm id`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val intent = Intent(app, AlarmRingingActivity::class.java).apply {
            putExtra(Constants.EXTRA_ALARM_ID, 10L)
        }
        val controller = Robolectric.buildActivity(AlarmRingingActivity::class.java, intent).create()
        val activity = controller.get()

        closeReceiver(activity).onReceive(activity, Intent(Constants.ACTION_ALARM_DISMISS).apply {
            putExtra(Constants.EXTRA_ALARM_ID, 11L)
        })

        assertFalse(activity.isFinishing)
    }

    @Test
    fun `launch starts activity with expected extras`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowContext = shadowOf(context)
        while (shadowContext.nextStartedActivity != null) {}

        AlarmRingingActivity.launch(context, alarmId = 42L, ringInActivity = true)

        val started = shadowContext.nextStartedActivity
        assertNotNull(started)
        assertEquals(AlarmRingingActivity::class.java.name, started?.component?.className)
        assertEquals(42L, started?.getLongExtra(Constants.EXTRA_ALARM_ID, -1L))
        assertTrue(started?.getBooleanExtra(Constants.EXTRA_RING_IN_ACTIVITY, false) == true)
        assertTrue(started != null && (started.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun `pendingIntent includes alarm id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val pendingIntent = AlarmRingingActivity.pendingIntent(context, 77L)
        val shadowPendingIntent = shadowOf(pendingIntent)
        val savedIntent = shadowPendingIntent.savedIntent

        assertEquals(AlarmRingingActivity::class.java.name, savedIntent.component?.className)
        assertEquals(77L, savedIntent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L))
    }

    private fun closeReceiver(activity: AlarmRingingActivity): BroadcastReceiver {
        val field = AlarmRingingActivity::class.java.getDeclaredField("closeReceiver")
        field.isAccessible = true
        return field.get(activity) as BroadcastReceiver
    }
}
