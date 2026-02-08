package com.sleep8.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sleep8.domain.overlay.AlarmOverlayPolicy
import com.sleep8.ui.theme.Sleep8Theme
import com.sleep8.util.PermissionUtils
import com.sleep8.util.TimeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Sleep8Theme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onThemeChanged = { recreate() }
                )
            }
        }
    }
}

private data class SettingsSectionModel(
    val title: String,
    val content: @Composable ColumnScope.() -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onThemeChanged: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val defaultDurationMinutes = com.sleep8.util.Constants.ALARM_DEFAULT_DURATION_MINUTES
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshReliability(context)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshReliability(context)
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshReliability(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sections = listOf(
        SettingsSectionModel(title = "Appearance") {
            RowWithSwitch(
                label = "Dark mode",
                checked = uiState.darkModeEnabled,
                onCheckedChange = {
                    viewModel.updateDarkModeEnabled(it)
                    onThemeChanged()
                }
            )
        },
        SettingsSectionModel(title = "Night Window") {
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
        },
        SettingsSectionModel(title = "Alarm Behavior") {
            AlarmDurationFields(
                hours = uiState.alarmDurationHoursInput,
                minutes = uiState.alarmDurationMinutesInput,
                error = uiState.alarmDurationError,
                onHoursChanged = viewModel::updateAlarmDurationHours,
                onMinutesChanged = viewModel::updateAlarmDurationMinutes,
                onReset = {
                    val (hours, minutes) = com.sleep8.util.AlarmDurationValidator.split(defaultDurationMinutes)
                    viewModel.updateAlarmDurationHours(hours.toString())
                    viewModel.updateAlarmDurationMinutes(minutes.toString())
                }
            )

            OutlinedTextField(
                value = uiState.confirmOffMinutes,
                onValueChange = viewModel::updateConfirmOffMinutes,
                label = { Text("Confirm off window (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Time allowed to confirm you're awake") }
            )

            RowWithSwitch(
                label = "Use overlay for alarm UI (more reliable)",
                checked = uiState.overlayEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updateOverlayEnabled(enabled)
                    val overlayAllowed = PermissionUtils.canDrawOverlays(context)
                    if (AlarmOverlayPolicy.shouldPromptForPermission(enabled, overlayAllowed)) {
                        context.startActivity(PermissionUtils.overlayIntent(context))
                    }
                }
            )
        },
        SettingsSectionModel(title = "System Reliability") {
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
                label = "Notifications",
                ok = uiState.notificationsAllowed,
                actionText = "Enable",
                description = "Needed for alarm notifications and lockscreen UI.",
                onAction = {
                    if (PermissionUtils.needsPostNotifications(context)) {
                        viewModel.setNotificationsAsked()
                        notificationPermissionLauncher.launch(PermissionUtils.notificationsPermission())
                    }
                }
            )

            ChecklistRow(
                label = "Full-screen alarm UI",
                ok = uiState.fullScreenIntentAllowed,
                actionText = "Allow",
                description = "Allows Sleep8 to open the alarm screen over the lock screen.",
                onAction = {
                    context.startActivity(PermissionUtils.fullScreenIntentSettingsIntent(context))
                }
            )

            ChecklistRow(
                label = "Battery optimization",
                ok = uiState.batteryOptimizationsIgnored,
                actionText = "Request exclusion",
                description = "On Pixel, set Sleep8 to Unrestricted battery and disable Extreme Battery Saver for Sleep8 so monitoring can auto-start at night window start.",
                onAction = {
                    context.startActivity(PermissionUtils.batteryOptimizationIntent(context))
                    viewModel.setBatteryOptAck(true)
                }
            )

            ChecklistRow(
                label = "Draw over other apps (optional)",
                ok = uiState.overlayAllowed,
                actionText = "Allow",
                description = "Optional overlay support when alarm is ringing.",
                onAction = {
                    context.startActivity(PermissionUtils.overlayIntent(context))
                }
            )

            Button(
                onClick = { viewModel.refreshReliability(context) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "Refresh status")
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("settings-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sections) { section ->
                SettingsSectionCard(title = section.title, content = section.content)
            }
        }
    }
}

@Composable
fun AlarmDurationFields(
    hours: String,
    minutes: String,
    error: String?,
    onHoursChanged: (String) -> Unit,
    onMinutesChanged: (String) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Alarm duration",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = hours,
                onValueChange = onHoursChanged,
                label = { Text("Hours") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("alarm-duration-hours"),
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = minutes,
                onValueChange = onMinutesChanged,
                label = { Text("Minutes") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("alarm-duration-minutes"),
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        val message = error ?: "Allowed range: 0-720 minutes"
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (error == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )
        Button(onClick = onReset) {
            Text("Reset to default")
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            content()
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            Text(text = "Change")
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
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
