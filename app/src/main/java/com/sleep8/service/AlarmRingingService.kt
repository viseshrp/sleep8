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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sleep8.R
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.ui.alarm.AlarmActivity
import com.sleep8.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmRingingService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var alarmId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alarmId = intent?.getLongExtra(Constants.EXTRA_ALARM_ID, -1L) ?: -1L
        when (intent?.action) {
            Constants.ACTION_ALARM_DISMISS -> {
                stopRinging()
                stopSelf()
                return START_NOT_STICKY
            }
            Constants.ACTION_ALARM_SNOOZE -> {
                stopRinging()
                stopSelf()
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
        val dismissIntent = createActionIntent(Constants.ACTION_ALARM_DISMISS)
        val notification = NotificationCompat.Builder(this, Constants.ALARM_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(getString(R.string.alarm_notification_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(alarmIntent, true)
            .addAction(R.drawable.ic_tile, getString(R.string.alarm_dismiss), dismissIntent)
            .setOngoing(true)
            .build()

        notificationHelper.ensureAlarmChannel()
        startForeground(Constants.ALARM_NOTIFICATION_ID, notification)
        startAudio()
        startVibration()
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

    private fun createActionIntent(action: String): android.app.PendingIntent {
        val intent = Intent(this, AlarmRingingService::class.java).apply {
            this.action = action
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
