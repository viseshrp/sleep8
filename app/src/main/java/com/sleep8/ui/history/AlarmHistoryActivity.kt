package com.sleep8.ui.history

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleep8.domain.model.AlarmCancelReason
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.ui.theme.Sleep8Theme
import com.sleep8.util.AlarmIntents
import com.sleep8.util.TimeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmHistoryActivity : AppCompatActivity() {

    private val viewModel: AlarmHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            Sleep8Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmHistoryScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            AlarmClock.ACTION_SHOW_ALARMS,
            Intent.ACTION_VIEW -> {
                viewModel.loadAlarm(AlarmIntents.parseAlarmId(intent.data))
            }

            else -> viewModel.loadAlarm(null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmHistoryScreen(
    viewModel: AlarmHistoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Alarm History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.alarms.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirmDialog = true }) {
                            Text("Clear")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.alarms.isEmpty()) {
            Text(
                text = "No alarms recorded yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.selectedAlarm?.let { selected ->
                    item {
                        AlarmDetailCard(alarm = selected)
                    }
                }
                items(uiState.alarms, key = { it.id }) { alarm ->
                    AlarmHistoryRow(alarm = alarm)
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear alarm history?") },
            text = { Text("This will permanently remove all alarm records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AlarmDetailCard(alarm: AlarmRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Alarm Detail", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            DetailLine(label = "Scheduled", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.triggerAt)))
            DetailLine(label = "Duration", value = TimeUtils.formatDurationMinutes(alarm.durationUsedMinutes))
            DetailLine(
                label = "Status",
                value = alarm.status.name.lowercase().replaceFirstChar { it.uppercase() }
            )
            alarm.canceledReason?.let { DetailLine(label = "Canceled", value = formatCancelReason(it)) }
            alarm.firedAt?.let {
                DetailLine(label = "Fired", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it)))
            }
            alarm.dismissedAt?.let {
                DetailLine(label = "Dismissed", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it)))
            }
        }
    }
}

@Composable
private fun AlarmHistoryRow(alarm: AlarmRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DetailLine(label = "Screen off", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.screenOffTs)))
            DetailLine(label = "Confirmed", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.confirmedAt)))
            DetailLine(label = "Alarm", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(alarm.triggerAt)))
            DetailLine(
                label = "Status",
                value = alarm.status.name.lowercase().replaceFirstChar { it.uppercase() }
            )
            alarm.canceledReason?.let { DetailLine(label = "Canceled", value = formatCancelReason(it)) }
            DetailLine(label = "Duration", value = TimeUtils.formatDurationMinutes(alarm.durationUsedMinutes))
            alarm.firedAt?.let {
                DetailLine(label = "Fired", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it)))
            }
            alarm.dismissedAt?.let {
                DetailLine(label = "Dismissed", value = TimeUtils.formatAlarmTime(TimeUtils.toLocalTime(it)))
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatCancelReason(reason: AlarmCancelReason): String {
    return when (reason) {
        AlarmCancelReason.REPLACED_BY_NEW_ALARM -> "Replaced by new alarm"
        AlarmCancelReason.USER_DISARM -> "Canceled on disarm"
        AlarmCancelReason.USER_TOGGLE_OFF -> "Disabled by user"
        AlarmCancelReason.REBOOT_CLEANUP -> "Cleaned up after reboot"
    }
}
