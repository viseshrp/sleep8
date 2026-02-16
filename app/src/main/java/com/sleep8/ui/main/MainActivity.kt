package com.sleep8.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.ui.alarm.AlarmListItem
import com.sleep8.ui.alarm.AlarmListRow
import com.sleep8.ui.alarm.AlarmListViewModel
import com.sleep8.ui.components.ArmButton
import com.sleep8.ui.components.StatusCard
import com.sleep8.ui.history.AlarmHistoryActivity
import com.sleep8.ui.settings.SettingsActivity
import com.sleep8.ui.theme.Sleep8Theme
import com.sleep8.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val alarmListViewModel: AlarmListViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        appPreferences.notificationsAsked = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !viewModel.startupReady.value }

        setContent {
            Sleep8Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        alarmListViewModel = alarmListViewModel,
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                        onOpenHistory = { startActivity(Intent(this, AlarmHistoryActivity::class.java)) },
                        onToggleArmed = { handleArmToggle() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshOnResume()
        alarmListViewModel.refresh()
    }

    private fun handleArmToggle() {
        if (PermissionUtils.needsPostNotifications(this) && !appPreferences.notificationsAsked) {
            appPreferences.notificationsAsked = true
            notificationPermissionLauncher.launch(PermissionUtils.notificationsPermission())
        }
        viewModel.toggleArmed()
    }
}

@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    alarmListViewModel: AlarmListViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleArmed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val alarmListUiState by alarmListViewModel.uiState.collectAsState()
    MainContent(
        uiState = uiState,
        alarmItems = alarmListUiState.items,
        updatingAlarmIds = alarmListUiState.updatingIds,
        onOpenSettings = onOpenSettings,
        onOpenHistory = onOpenHistory,
        onToggleAlarm = alarmListViewModel::onToggle,
        onToggleArmed = onToggleArmed
    )
}

private data class HomeAction(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainContent(
    uiState: MainUiState,
    alarmItems: List<AlarmListItem>,
    updatingAlarmIds: Set<Long>,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    onToggleArmed: () -> Unit
) {
    val recentAlarmItems = alarmItems.take(1)
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    val actions = listOf(
        HomeAction(
            title = "Alarm history",
            subtitle = "Review recent alarm events",
            onClick = onOpenHistory
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Sleep8",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Alarm History") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenHistory()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Sleep8") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("main-menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open menu"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                ArmButton(
                    armed = uiState.armed,
                    onToggle = onToggleArmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Tonight",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Sleep8 monitors screen-off activity and schedules your wake alarm automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }


                item {
                    StatusCard(
                        status = uiState.statusText,
                        windowEnds = uiState.windowEndsText,
                        lastScreenOff = uiState.lastScreenOffText,
                        monitoringHealthText = uiState.monitoringHealthText,
                        latestAlarmText = uiState.latestAlarmText,
                        latestAlarmSubtitle = uiState.latestAlarmSubtitle,
                        notificationWarningText = uiState.notificationWarningText,
                        reliabilityWarningText = uiState.reliabilityWarningText,
                        pendingCountdown = if (uiState.showPending) uiState.pendingCountdownText else null
                    )
                }

                item {
                    Text(
                        text = "Recent alarms",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }

                item {
                    if (recentAlarmItems.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "No alarms yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                            )
                        }
                    }
                }

                if (recentAlarmItems.isNotEmpty()) {
                    itemsIndexed(recentAlarmItems, key = { _, item -> item.id }) { _, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            AlarmListRow(
                                item = item,
                                isUpdating = updatingAlarmIds.contains(item.id),
                                onToggle = { enabled -> onToggleAlarm(item.id, enabled) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Quick access",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }

                items(actions) { action ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = MaterialTheme.shapes.medium,
                        onClick = action.onClick
                    ) {

                        ListItem(
                            headlineContent = { Text(action.title) },
                            supportingContent = { Text(action.subtitle) },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
