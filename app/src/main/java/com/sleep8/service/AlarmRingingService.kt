package com.sleep8.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.RingtoneManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.sleep8.R
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.data.repository.SettingsRepository
import com.sleep8.domain.scheduler.AlarmScheduler
import com.sleep8.domain.overlay.AlarmOverlayPolicy
import com.sleep8.service.notification.AlarmNotificationFactory
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.service.overlay.AlarmOverlayController
import com.sleep8.ui.alarm.AlarmActivity
import com.sleep8.util.AlarmIntents
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
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
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
            Constants.ACTION_ALARM_SNOOZE -> {
                handleSnooze()
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
        val alarmIntent = AlarmActivity.pendingIntent(this, alarmId)
        val contentIntent = AlarmIntents.alarmDetailPendingIntent(
            this,
            alarmId.toInt(),
            alarmId
        )
        val dismissIntent = createActionIntent(Constants.ACTION_ALARM_DISMISS)
        val snoozeEnabled = runBlocking {
            settingsRepository.getSettings().snoozeMinutes != null
        }
        val snoozeIntent = if (snoozeEnabled) createActionIntent(Constants.ACTION_ALARM_SNOOZE) else null
        val notification = AlarmNotificationFactory(this).buildRingingNotification(
            alarmIntent = alarmIntent,
            contentIntent = contentIntent,
            dismissIntent = dismissIntent,
            snoozeIntent = snoozeIntent
        )

        notificationHelper.ensureAlarmRingingChannel()
        try {
            startForeground(Constants.ALARM_RINGING_NOTIFICATION_ID, notification)
        } catch (exception: SecurityException) {
            Log.e("AlarmRingingService", "Notification permission denied; running without foreground notification.", exception)
        }
        startAudio()
        startVibration()
        maybeShowOverlay(snoozeEnabled)
    }

    private fun startAudio() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { }
                .build()
            audioManager?.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN)
        }

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val soundUri = alarmUri ?: fallbackUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (soundUri != null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@AlarmRingingService, soundUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (_: Exception) {
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopRinging() {
        overlayController?.dismiss()
        overlayController = null
        mediaPlayer?.run {
            stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
        focusRequest = null
    }

    private fun handleDismiss() {
        stopRinging()
        markDismissed()
        broadcastAlarmAction(Constants.ACTION_ALARM_DISMISS)
        stopSelf()
    }

    private fun handleSnooze() {
        stopRinging()
        scheduleSnooze()
        broadcastAlarmAction(Constants.ACTION_ALARM_SNOOZE)
        stopSelf()
    }

    private fun markDismissed() {
        if (alarmId <= 0) return
        serviceScope.launch {
            alarmRepository.markDismissed(alarmId, System.currentTimeMillis())
        }
    }

    private fun scheduleSnooze() {
        if (alarmId <= 0) return
        serviceScope.launch {
            val settings = settingsRepository.getSettings()
            val minutes = settings.snoozeMinutes ?: return@launch
            alarmScheduler.scheduleSnooze(alarmId, minutes)
        }
    }

    private fun broadcastAlarmAction(action: String) {
        val intent = Intent(action).apply {
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
    }

    private fun maybeShowOverlay(showSnooze: Boolean) {
        if (alarmId <= 0) return
        val settings = runBlocking { settingsRepository.getSettings() }
        val overlayAllowed = PermissionUtils.canDrawOverlays(this)
        if (!AlarmOverlayPolicy.shouldShowOverlay(settings.overlayEnabled, overlayAllowed)) return
        overlayController = AlarmOverlayController(this).also { controller ->
            controller.show(
                showSnooze = showSnooze,
                onDismiss = { handleDismiss() },
                onSnooze = { handleSnooze() }
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
