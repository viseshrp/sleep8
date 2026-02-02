package com.sleep8.ui.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.sleep8.service.AlarmRingingService
import com.sleep8.util.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels()
    private var alarmId: Long = -1L

    private val closeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action != Constants.ACTION_ALARM_DISMISS && action != Constants.ACTION_ALARM_SNOOZE) return
            val targetId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
            if (alarmId > 0 && targetId > 0 && targetId != alarmId) return
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        viewModel.setAlarmId(alarmId)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmScreen(
                        viewModel = viewModel,
                        onDismiss = {
                            viewModel.dismiss()
                            AlarmRingingService.stop(this)
                            finish()
                        },
                        onSnooze = {
                            viewModel.snooze()
                            AlarmRingingService.stop(this)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_ALARM_DISMISS)
            addAction(Constants.ACTION_ALARM_SNOOZE)
        }
        registerReceiver(closeReceiver, filter)
    }

    override fun onStop() {
        try {
            unregisterReceiver(closeReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver already unregistered
        }
        super.onStop()
    }

    companion object {
        fun launch(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            }
            context.startActivity(intent)
        }

        fun pendingIntent(context: Context, alarmId: Long): PendingIntent {
            val intent = Intent(context, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            }
            return PendingIntent.getActivity(
                context,
                Constants.PENDING_INTENT_REQUEST_ALARM_ACTION,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

@Composable
private fun AlarmScreen(
    viewModel: AlarmViewModel,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFF120F0A),
            Color(0xFF3A1F0E),
            Color(0xFF5B2C10)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = com.sleep8.R.string.alarm_ui_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFE9D2)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = uiState.currentTime,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD7A0)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Dismiss")
            }
            if (uiState.showSnooze) {
                Button(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Snooze")
                }
            }
        }
    }
}
