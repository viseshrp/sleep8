package com.sleep8.ui.history

import android.content.Intent
import android.provider.AlarmClock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmHistoryActivityTest {

    @Test
    fun `ACTION_SHOW_ALARMS launches history activity`() {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        Robolectric.buildActivity(AlarmHistoryActivity::class.java, intent).setup().get()
    }
}
