package com.sleep8.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sleep8.ui.main.MainActivity
import com.sleep8.util.PermissionUtils
import com.sleep8.util.TimeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshReliability(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        }

        TimePickerRow(
            label = "Night start",
            value = uiState.nightStart,
            onValueSelected = viewModel::updateNightStart
        )
        Spacer(modifier = Modifier.height(12.dp))
        TimePickerRow(
            label = "Night end",
            value = uiState.nightEnd,
            onValueSelected = viewModel::updateNightEnd
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.alarmOffsetHours,
            onValueChange = viewModel::updateAlarmOffset,
            label = { Text("Alarm lead time (hours)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Snooze")
        RowWithSwitch(
            label = "Enable snooze",
            checked = uiState.snoozeEnabled,
            onCheckedChange = { viewModel.updateSnooze(it, uiState.snoozeMinutes) }
        )
        if (uiState.snoozeEnabled) {
            OutlinedTextField(
                value = uiState.snoozeMinutes,
                onValueChange = { viewModel.updateSnooze(true, it) },
                label = { Text("Snooze minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Reliability checklist", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ChecklistRow(
            label = "Exact alarms",
            ok = uiState.exactAlarmAllowed,
            actionText = "Grant",
            onAction = {
                context.startActivity(PermissionUtils.exactAlarmIntent(context))
            }
        )

        ChecklistRow(
            label = "Battery optimization",
            ok = uiState.batteryOptimizationsIgnored,
            actionText = "Request exclusion",
            onAction = {
                context.startActivity(PermissionUtils.batteryOptimizationIntent(context))
                viewModel.setBatteryOptAck(true)
            }
        )

        ChecklistRow(
            label = "Foreground service",
            ok = uiState.foregroundServiceActive,
            actionText = "Open app",
            onAction = { context.startActivity(android.content.Intent(context, MainActivity::class.java)) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.refreshReliability(context) }) {
            Text(text = "Refresh Status")
        }
    }
}

@Composable
private fun TimePickerRow(
    label: String,
    value: String,
    onValueSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val time = remember(value) { TimeUtils.parseLocalTime(value) }
    var display by remember(value) { mutableStateOf(value) }

    Button(onClick = {
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                val formatted = String.format("%02d:%02d", hour, minute)
                display = formatted
                onValueSelected(formatted)
            },
            time.hour,
            time.minute,
            true
        )
        dialog.show()
    }) {
        Text(text = "$label: $display")
    }
}

@Composable
private fun RowWithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    ok: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label: ${if (ok) "OK" else "Needs attention"}")
        Spacer(modifier = Modifier.width(12.dp))
        if (!ok) {
            Button(onClick = onAction) {
                Text(text = actionText)
            }
        }
    }
}
