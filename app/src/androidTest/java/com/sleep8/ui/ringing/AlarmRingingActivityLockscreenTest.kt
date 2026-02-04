package com.sleep8.ui.ringing

import android.content.Intent
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sleep8.util.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmRingingActivityLockscreenTest {

    private val launchIntent = Intent(
        ApplicationProvider.getApplicationContext(),
        AlarmRingingActivity::class.java
    ).apply {
        putExtra(Constants.EXTRA_ALARM_ID, 123L)
    }

    @get:Rule
    val activityRule = ActivityScenarioRule<AlarmRingingActivity>(launchIntent)

    @Test
    fun ringingActivity_keepsScreenOn_andStaysActiveWithAlarmId() {
        activityRule.scenario.onActivity { activity ->
            assertFalse(activity.isFinishing)
            assertFalse(activity.isDestroyed)
            assertTrue(
                (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
            )
        }
    }
}
