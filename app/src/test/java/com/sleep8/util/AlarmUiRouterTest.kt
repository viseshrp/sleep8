package com.sleep8.util

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.ui.alarm.AlarmActivity
import com.sleep8.ui.history.AlarmHistoryActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmUiRouterTest {

    @Test
    fun `ringing routes to AlarmActivity with active id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = AlarmUiRouter.buildIntent(
            context = context,
            isRinging = true,
            activeAlarmId = 42L,
            latestAlarmId = 5L
        )

        assertEquals(AlarmActivity::class.java.name, intent.component?.className)
        assertEquals(42L, intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L))
    }

    @Test
    fun `not ringing routes to alarm detail when available`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = AlarmUiRouter.buildIntent(
            context = context,
            isRinging = false,
            activeAlarmId = null,
            latestAlarmId = 7L
        )

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(AlarmIntents.alarmDetailUri(7L), intent.data)
    }

    @Test
    fun `not ringing routes to history when no alarms`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = AlarmUiRouter.buildIntent(
            context = context,
            isRinging = false,
            activeAlarmId = null,
            latestAlarmId = null
        )

        assertNotNull(intent.component)
        assertEquals(AlarmHistoryActivity::class.java.name, intent.component?.className)
    }
}
