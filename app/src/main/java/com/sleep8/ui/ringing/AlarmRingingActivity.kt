package com.sleep8.ui.ringing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import com.sleep8.service.AlarmRinger
import com.sleep8.service.AlarmRingingService
import com.sleep8.ui.theme.Sleep8Theme
import com.sleep8.util.TimeUtils
import com.sleep8.util.Constants
import java.time.LocalTime

class AlarmRingingActivity : AppCompatActivity() {

    private var alarmId: Long = -1L
    private var ringInActivity: Boolean = false
    private var ringer: AlarmRinger? = null

    private val closeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action != Constants.ACTION_ALARM_DISMISS) return
            val targetId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
            if (alarmId > 0 && targetId > 0 && targetId != alarmId) return
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        if (!applyIntent(intent, allowExistingAlarmId = false)) {
            finish()
            return
        }

        setContent {
            Sleep8Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmRingingContent(
                        label = getString(com.sleep8.R.string.alarm_ringing_title),
                        alarmInfo = "${TimeUtils.formatAlarmTime(LocalTime.now())}",
                        onDismiss = {
                            ringer?.stop()
                            AlarmRingingService.stop(this, alarmId)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!applyIntent(intent, allowExistingAlarmId = true)) {
            finish()
            return
        }
        maybeStartInActivityRinger()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_ALARM_DISMISS)
        }
        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        maybeStartInActivityRinger()
    }

    override fun onStop() {
        try {
            unregisterReceiver(closeReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver already unregistered
        }
        ringer?.stop()
        ringer = null
        super.onStop()
    }

    private fun applyIntent(intent: Intent, allowExistingAlarmId: Boolean): Boolean {
        val incomingAlarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        if (incomingAlarmId > 0) {
            alarmId = incomingAlarmId
        } else if (!allowExistingAlarmId || alarmId <= 0) {
            return false
        }
        ringInActivity = intent.getBooleanExtra(Constants.EXTRA_RING_IN_ACTIVITY, ringInActivity)
        return true
    }

    private fun maybeStartInActivityRinger() {
        if (ringInActivity && ringer == null) {
            ringer = AlarmRinger(this).also { it.start() }
        }
    }

    companion object {
        fun launch(context: Context, alarmId: Long, ringInActivity: Boolean = false) {
            val intent = Intent(context, AlarmRingingActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(Constants.EXTRA_ALARM_ID, alarmId)
                putExtra(Constants.EXTRA_RING_IN_ACTIVITY, ringInActivity)
            }
            context.startActivity(intent)
        }

        fun pendingIntent(context: Context, alarmId: Long): PendingIntent {
            val intent = Intent(context, AlarmRingingActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            }
            return PendingIntent.getActivity(
                context,
                Constants.PENDING_INTENT_REQUEST_ALARM_RINGING_ACTIVITY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
