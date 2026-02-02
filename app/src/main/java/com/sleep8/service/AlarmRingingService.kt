package com.sleep8.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.sleep8.R
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.overlay.AlarmOverlayPolicy
import com.sleep8.service.notification.AlarmNotificationFactory
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.service.overlay.AlarmOverlayController
import com.sleep8.ui.ringing.AlarmRingingActivity
import com.sleep8.util.Constants
import com.sleep8.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class AlarmRingingService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appPreferences: AppPreferences

    private var ringer: AlarmRinger? = null
    private var alarmId: Long = -1L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var overlayController: AlarmOverlayController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alarmId = intent?.getLongExtra(Constants.EXTRA_ALARM_ID, -1L) ?: -1L
        when (intent?.action) {
            Constants.ACTION_ALARM_DISMISS -> {
                handleDismiss()
                return START_NOT_STICKY
            }
            else -> startRinging()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    private fun startRinging() {
        appPreferences.activeAlarmId = if (alarmId > 0) alarmId else -1L
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
        if (ringer == null) {
            ringer = AlarmRinger(this)
        }
        ringer?.start()
        maybeShowOverlay()
    }

    private fun stopRinging() {
        overlayController?.dismiss()
        overlayController = null
        appPreferences.activeAlarmId = -1L
        appPreferences.activeAlarmRequestCode = -1
        appPreferences.activeAlarmInstanceId = -1L
        ringer?.stop()
        ringer = null
    }

    private fun handleDismiss() {
        stopRinging()
        markDismissed()
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
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
    }

    private fun maybeShowOverlay() {
        if (alarmId <= 0) return
        val settings = runBlocking { settingsRepository.getSettings() }
        val overlayAllowed = PermissionUtils.canDrawOverlays(this)
        if (!AlarmOverlayPolicy.shouldShowOverlay(settings.overlayEnabled, overlayAllowed)) return
        overlayController = AlarmOverlayController(this).also { controller ->
            controller.show(
                onDismiss = { handleDismiss() }
            )
        }
        serviceScope.launch {
            alarmRepository.markOverlayUsed(alarmId)
        }
    }

    private fun createActionIntent(action: String): android.app.PendingIntent {
        val intent = Intent(this, AlarmRingingService::class.java).apply {
            this.action = action
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        return android.app.PendingIntent.getService(
            this,
            Constants.PENDING_INTENT_REQUEST_ALARM_ACTION,
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

        fun stop(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = Constants.ACTION_ALARM_DISMISS
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
