package com.sleep8.util

import android.content.Context
import android.content.Intent
import com.sleep8.ui.alarm.AlarmActivity
import com.sleep8.ui.alarm.AlarmListActivity

object AlarmUiRouter {

    fun buildIntent(
        context: Context,
        isRinging: Boolean,
        activeAlarmId: Long?
    ): Intent {
        return if (isRinging && (activeAlarmId ?: -1L) > 0L) {
            Intent(context, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Constants.EXTRA_ALARM_ID, activeAlarmId)
            }
        } else {
            Intent(context, AlarmListActivity::class.java)
        }
    }
}
