package com.sleep8.ui.alarm

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.sleep8.R
import com.sleep8.ui.ringing.AlarmRingingActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmActivityTitleTest {

    @Test
    fun `ringing activity label uses alarm ringing title`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getActivityInfo(
            android.content.ComponentName(context, AlarmRingingActivity::class.java),
            PackageManager.GET_META_DATA
        )
        assertEquals(R.string.alarm_ringing_title, info.labelRes)
    }
}
