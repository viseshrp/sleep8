package com.sleep8.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshReliability(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section: Schedule
            SettingsSection(title = "Schedule") {
                TimePickerRow(
                    label = "Night start",
                    value = uiState.nightStart,
                    onValueSelected = viewModel::updateNightStart
                )
                TimePickerRow(
                    label = "Night end",
                    value = uiState.nightEnd,
                    onValueSelected = viewModel::updateNightEnd
                )
                RowWithSwitch(
                    label = "Auto-arm schedule",
                    checked = uiState.autoArmEnabled,
                    onCheckedChange = viewModel::updateAutoArmEnabled
                )
            }

            // Section: Alarm Behavior
            SettingsSection(title = "Alarm Behavior") {
                OutlinedTextField(
                    value = uiState.alarmOffsetHours,
                    onValueChange = viewModel::updateAlarmOffset,
                    label = { Text("Alarm lead time (hours)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("How long before 'Night end' to start checking screen status") }
                )

                OutlinedTextField(
                    value = uiState.confirmOffMinutes,
                    onValueChange = viewModel::updateConfirmOffMinutes,
                    label = { Text("Confirm off window (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Time allowed to confirm you're awake") }
                )

                Column {
                    RowWithSwitch(
                        label = "Enable snooze",
                        checked = uiState.snoozeEnabled,
                        onCheckedChange = { viewModel.updateSnooze(it, uiState.snoozeMinutes) }
                    )
                    if (uiState.snoozeEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.snoozeMinutes,
                            onValueChange = { viewModel.updateSnooze(true, it) },
                            label = { Text("Snooze duration (minutes)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // Section: Reliability
            SettingsSection(title = "System Reliability") {
                ChecklistRow(
                    label = "Exact alarms",
                    ok = uiState.exactAlarmAllowed,
                    actionText = "Grant",
                    description = "Required to trigger alarms precisely at the scheduled time.",
                    onAction = {
                        context.startActivity(PermissionUtils.exactAlarmIntent(context))
                    }
                )

                ChecklistRow(
                    label = "Battery optimization",
                    ok = uiState.batteryOptimizationsIgnored,
                    actionText = "Request exclusion",
                    description = "Prevents the system from killing the app during the night.",
                    onAction = {
                        context.startActivity(PermissionUtils.batteryOptimizationIntent(context))
                        viewModel.setBatteryOptAck(true)
                    }
                )

                Button(
                    onClick = { viewModel.refreshReliability(context) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "Refresh Status")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider()
        content()
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = {
            val dialog = TimePickerDialog(
                context,
                { _, hour, minute ->
                    val formatted = String.format("%02d:%02d", hour, minute)
                    onValueSelected(formatted)
                },
                time.hour,
                time.minute,
                true
            )
            dialog.show()
        }) {
            Text(text = value)
        }
    }
}

@Composable
private fun RowWithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    ok: Boolean,
    actionText: String,
    description: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (ok) "Status: OK" else "Status: Inactive",
                style = MaterialTheme.typography.labelSmall,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        if (!ok) {
            Button(onClick = onAction) {
                Text(text = actionText)
            }
        }
    }
}
