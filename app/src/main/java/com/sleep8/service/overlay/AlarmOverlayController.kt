package com.sleep8.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.sleep8.R
import com.sleep8.util.TimeUtils
import java.time.LocalTime

class AlarmOverlayController(
    private val context: Context
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var timeView: TextView? = null
    private var ticker: Runnable? = null

    fun show(showSnooze: Boolean, onDismiss: () -> Unit, onSnooze: () -> Unit) {
        if (overlayView != null) return
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_alarm, null)
        val dismissButton = view.findViewById<Button>(R.id.overlay_dismiss)
        val snoozeButton = view.findViewById<Button>(R.id.overlay_snooze)
        val timeLabel = view.findViewById<TextView>(R.id.overlay_time)
        timeView = timeLabel
        snoozeButton.visibility = if (showSnooze) View.VISIBLE else View.GONE

        dismissButton.setOnClickListener { onDismiss() }
        snoozeButton.setOnClickListener { onSnooze() }

        overlayView = view
        windowManager.addView(view, buildLayoutParams())
        startTicker()
    }

    fun dismiss() {
        val view = overlayView ?: return
        stopTicker()
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // View already removed
        }
        overlayView = null
        timeView = null
    }

    private fun startTicker() {
        val runnable = object : Runnable {
            override fun run() {
                val now = LocalTime.now()
                timeView?.text = TimeUtils.formatAlarmTime(now)
                handler.postDelayed(this, 1000)
            }
        }
        ticker = runnable
        handler.post(runnable)
    }

    private fun stopTicker() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }
}
