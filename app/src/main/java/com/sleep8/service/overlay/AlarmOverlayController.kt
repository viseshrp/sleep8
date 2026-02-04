package com.sleep8.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.sleep8.ui.ringing.AlarmRingingContent
import com.sleep8.ui.theme.Sleep8Theme

class AlarmOverlayController(
    private val context: Context
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun show(onDismiss: () -> Unit) {
        if (overlayView != null) return
        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                Sleep8Theme {
                    AlarmRingingContent(
                        label = context.getString(com.sleep8.R.string.alarm_ringing_title),
                        onDismiss = onDismiss
                    )
                }
            }
        }

        overlayView = view
        windowManager.addView(view, buildLayoutParams())
    }

    fun dismiss() {
        val view = overlayView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // View already removed
        }
        overlayView = null
    }

    @Suppress("DEPRECATION")
    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
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
