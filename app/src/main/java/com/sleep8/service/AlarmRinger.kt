package com.sleep8.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmRinger(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    fun start() {
        startAudio()
        startVibration()
    }

    fun stop() {
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

    private fun startAudio() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, soundUri)
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
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val hasVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.hasVibrator() == true
        } else {
            vibrator != null
        }
        if (!hasVibrator) return

        val pattern = longArrayOf(0, 1000, 1000)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: SecurityException) {
            // VIBRATE permission missing; ignore vibration.
        }
    }
}
