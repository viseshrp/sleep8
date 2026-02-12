package com.sleep8.service

import android.app.Service
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.notification.AlarmNotificationFactory
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.ui.ringing.AlarmRingingActivity
import com.sleep8.util.Constants
import com.sleep8.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmRingingService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var stateHolder: StateHolder

    private var ringer: AlarmRinger? = null
    private var alarmId: Long = -1L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val incomingAlarmId = intent?.getLongExtra(Constants.EXTRA_ALARM_ID, -1L) ?: -1L

        when (action) {
            Constants.ACTION_ALARM_DISMISS -> {
                if (incomingAlarmId <= 0) {
                    Log.w("AlarmRingingService", "Ignoring dismiss action without a valid alarm id.")
                    return START_NOT_STICKY
                }
                if (alarmId > 0 && incomingAlarmId != alarmId) {
                    Log.w("AlarmRingingService", "Ignoring dismiss action for stale alarm id=$incomingAlarmId")
                    return START_NOT_STICKY
                }
                alarmId = incomingAlarmId
                handleDismiss()
                return START_NOT_STICKY
            }
            Constants.ACTION_ALARM_RING, null -> {
                if (incomingAlarmId > 0) {
                    if (alarmId > 0 && alarmId != incomingAlarmId) {
                        stopRinging()
                    }
                    alarmId = incomingAlarmId
                } else if (alarmId <= 0) {
                    alarmId = appPreferences.activeAlarmId
                }
                if (alarmId <= 0) {
                    Log.w("AlarmRingingService", "Cannot start ringing without a valid alarm id.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startRinging()
            }
            else -> {
                Log.w("AlarmRingingService", "Ignoring unknown action=${intent.action}")
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    private fun startRinging() {
        if (alarmId <= 0) {
            stopSelf()
            return
        }
        appPreferences.activeAlarmId = if (alarmId > 0) alarmId else -1L
        val deviceInUse = isDeviceInUse()
        val shouldLaunchActivity = !deviceInUse
        val alarmIntent = AlarmRingingActivity.pendingIntent(this, alarmId)
        val contentIntent = alarmIntent
        val dismissIntent = createActionIntent(Constants.ACTION_ALARM_DISMISS)
        val notification = AlarmNotificationFactory(this).buildRingingNotification(
            alarmIntent = alarmIntent,
            contentIntent = contentIntent,
            dismissIntent = dismissIntent
        )

        notificationHelper.ensureAlarmRingingChannel()
        if (PermissionUtils.needsPostNotifications(this) && !PermissionUtils.canPostNotifications(this)) {
            Log.w("AlarmRingingService", "Notification permission denied; skipping foreground service.")
            stopSelf()
            return
        }
        startForeground(Constants.ALARM_RINGING_NOTIFICATION_ID, notification)
        if (shouldLaunchActivity && alarmId > 0) {
            AlarmRingingActivity.launch(this, alarmId)
        }
        if (ringer == null) {
            ringer = AlarmRinger(this)
        }
        ringer?.start()
    }

    private fun isDeviceInUse(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val interactive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager?.isInteractive ?: true
        } else {
            @Suppress("DEPRECATION")
            powerManager?.isScreenOn ?: true
        }
        val keyguardLocked = keyguardManager?.isKeyguardLocked ?: false
        return interactive && !keyguardLocked
    }

    private fun stopRinging() {
        appPreferences.activeAlarmId = -1L
        appPreferences.activeAlarmRequestCode = -1
        appPreferences.activeAlarmInstanceId = -1L
        ringer?.stop()
        ringer = null
    }

    private fun handleDismiss() {
        stopRinging()
        markDismissed()
        stateHolder.clearLastScreenOffTs()
        broadcastAlarmAction(Constants.ACTION_ALARM_DISMISS)
        stopSelf()
    }

    private fun markDismissed() {
        if (alarmId <= 0) return
        serviceScope.launch {
            alarmRepository.markDismissed(alarmId, System.currentTimeMillis())
        }
    }

    private fun broadcastAlarmAction(action: String) {
        val intent = Intent(action).apply {
            `package` = packageName
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
    }

    private fun createActionIntent(action: String): android.app.PendingIntent {
        val intent = Intent(this, AlarmRingingService::class.java).apply {
            this.action = action
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        return android.app.PendingIntent.getService(
            this,
            Constants.PENDING_INTENT_REQUEST_ALARM_RINGING_DISMISS,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = Constants.ACTION_ALARM_RING
                putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = Constants.ACTION_ALARM_DISMISS
                putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
