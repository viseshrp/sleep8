package com.sleep8.ui.history

import android.os.Bundle
import android.provider.AlarmClock
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.sleep8.domain.model.AlarmCancelReason
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.util.AlarmIntents
import com.sleep8.util.TimeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmHistoryActivity : ComponentActivity() {

    private val viewModel: AlarmHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmHistoryScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: android.content.Intent) {
        when (intent.action) {
            AlarmClock.ACTION_SHOW_ALARMS,
            android.content.Intent.ACTION_VIEW -> {
                viewModel.loadAlarm(AlarmIntents.parseAlarmId(intent.data))
            }
            else -> viewModel.loadAlarm(null)
        }
    }
}

@Composable
private fun AlarmHistoryScreen(
    viewModel: AlarmHistoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0B1020),
            Color(0xFF101C2E),
            Color(0xFF12283A)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFE9EEF7)
                )
            }
            Text(
                text = "Alarm History",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFE9EEF7)
            )
        }

        if (uiState.alarms.isEmpty()) {
            Text(
                text = "No alarms recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8C3D6)
            )
        } else {
            uiState.selectedAlarm?.let { selected ->
                AlarmDetailCard(alarm = selected)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.alarms) { alarm ->
                    AlarmHistoryRow(alarm = alarm)
                }
            }
        }
    }
}

@Composable
private fun AlarmDetailCard(alarm: AlarmRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1AFFFFFF), shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "Alarm Detail", style = MaterialTheme.typography.titleMedium, color = Color(0xFFE9EEF7))
        Text(
            text = "Scheduled for ${TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.triggerAt))}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8C3D6)
        )
        Text(
            text = "Duration ${TimeUtils.formatDurationMinutes(alarm.durationUsedMinutes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8C3D6)
        )
        Text(
            text = "Status: ${alarm.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8C3D6)
        )
        alarm.canceledReason?.let {
            Text(
                text = "Canceled: ${formatCancelReason(it)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8C3D6)
            )
        }
        alarm.firedAt?.let {
            Text(
                text = "Fired ${TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it))}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8C3D6)
            )
        }
        alarm.dismissedAt?.let {
            Text(
                text = "Dismissed ${TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it))}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8C3D6)
            )
        }
    }
}

@Composable
private fun AlarmHistoryRow(alarm: AlarmRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1AFFFFFF), shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Screen off", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
            Text(
                text = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.screenOffTs)),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE9EEF7)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Confirmed", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
            Text(
                text = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.confirmedAt)),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE9EEF7)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Alarm", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
            Text(
                text = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.triggerAt)),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE9EEF7)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Status", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
            Text(
                text = alarm.status.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE9EEF7)
            )
        }
        alarm.canceledReason?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Canceled", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
                Text(
                    text = formatCancelReason(it),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE9EEF7)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Duration", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
            Text(
                text = TimeUtils.formatDurationMinutes(alarm.durationUsedMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE9EEF7)
            )
        }
        alarm.firedAt?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Fired", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
                Text(
                    text = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE9EEF7)
                )
            }
        }
        alarm.dismissedAt?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Dismissed", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
                Text(
                    text = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE9EEF7)
                )
            }
        }
    }
}

private fun formatCancelReason(reason: AlarmCancelReason): String {
    return when (reason) {
        AlarmCancelReason.REPLACED_BY_NEW_ALARM -> "Replaced by new alarm"
        AlarmCancelReason.USER_DISARM -> "Canceled on disarm"
        AlarmCancelReason.SNOOZE_REPLACE -> "Replaced by snooze"
        AlarmCancelReason.REBOOT_CLEANUP -> "Cleaned up after reboot"
    }
}
