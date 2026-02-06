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
import com.sleep8.service.AlarmRingingService
import com.sleep8.ui.components.ArmButton
import com.sleep8.ui.components.StatusCard
import com.sleep8.ui.history.AlarmHistoryActivity
import com.sleep8.ui.settings.SettingsActivity
import com.sleep8.ui.theme.Sleep8Theme
import com.sleep8.util.AlarmUiRouter
import com.sleep8.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

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
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                        onOpenAlarm = { openAlarmUi() },
                        onOpenHistory = { startActivity(Intent(this, AlarmHistoryActivity::class.java)) },
                        onToggleArmed = { handleArmToggle() }
                    )
                }
            }
        }
    }

    private fun handleArmToggle() {
        if (PermissionUtils.needsPostNotifications(this) && !appPreferences.notificationsAsked) {
            appPreferences.notificationsAsked = true
            notificationPermissionLauncher.launch(PermissionUtils.notificationsPermission())
        }
        viewModel.toggleArmed()
    }

    private fun openAlarmUi() {
        val isRinging = PermissionUtils.isServiceRunning(this, AlarmRingingService::class.java)
        val intent = AlarmUiRouter.buildIntent(
            this,
            isRinging = isRinging,
            activeAlarmId = appPreferences.activeAlarmId
        )
        startActivity(intent)
    }
}

@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenAlarm: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleArmed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    MainContent(
        uiState = uiState,
        onOpenSettings = onOpenSettings,
        onOpenAlarm = onOpenAlarm,
        onOpenHistory = onOpenHistory,
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
    onOpenSettings: () -> Unit,
    onOpenAlarm: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleArmed: () -> Unit
) {
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    val actions = listOf(
        HomeAction(
            title = "Alarm list",
            subtitle = "Manage scheduled alarms",
            onClick = onOpenAlarm
        ),
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
                    label = { Text("Alarm") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenAlarm()
                    },
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "Tonight", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Sleep8 monitors screen-off activity and schedules your wake alarm automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    StatusCard(
                        status = uiState.statusText,
                        armedUntil = uiState.armedUntilText,
                        lastScreenOff = uiState.lastScreenOffText,
                        latestAlarmText = uiState.latestAlarmText,
                        latestAlarmSubtitle = uiState.latestAlarmSubtitle,
                        notificationWarningText = uiState.notificationWarningText,
                        monitoringHealthText = uiState.monitoringHealthText,
                        reliabilityWarningText = uiState.reliabilityWarningText,
                        pendingCountdown = if (uiState.showPending) uiState.pendingCountdownText else null
                    )
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
