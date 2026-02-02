package com.sleep8.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.ui.alarm.AlarmActivity
import com.sleep8.ui.alarm.AlarmListActivity
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
            activeAlarmId = 42L
        )

        assertEquals(AlarmActivity::class.java.name, intent.component?.className)
        assertEquals(42L, intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L))
    }

    @Test
    fun `not ringing routes to alarm list`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = AlarmUiRouter.buildIntent(
            context = context,
            isRinging = false,
            activeAlarmId = null
        )

        assertNotNull(intent.component)
        assertEquals(AlarmListActivity::class.java.name, intent.component?.className)
    }
}
