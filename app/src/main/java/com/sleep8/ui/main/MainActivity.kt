package com.sleep8.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sleep8.ui.components.ArmButton
import com.sleep8.ui.components.StatusCard
import com.sleep8.ui.history.AlarmHistoryActivity
import com.sleep8.ui.settings.SettingsActivity
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var appPreferences: AppPreferences

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        appPreferences.notificationsAsked = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
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
}

@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleArmed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0B1020),
            Color(0xFF101C2E),
            Color(0xFF12283A)
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Sleep8",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(20.dp)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open menu",
                            tint = Color(0xFFE9EEF7)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.sleep8.R.mipmap.ic_launcher_foreground),
                        contentDescription = "Sleep8",
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .size(96.dp)
                    )
                    Text(
                        text = "Sleep8",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFE9EEF7)
                    )
                }

                StatusCard(
                    status = uiState.statusText,
                    armedUntil = uiState.armedUntilText,
                    lastScreenOff = uiState.lastScreenOffText,
                    latestAlarmText = uiState.latestAlarmText,
                    latestAlarmSubtitle = uiState.latestAlarmSubtitle,
                    systemNextAlarmText = uiState.systemNextAlarmText,
                    notificationWarningText = uiState.notificationWarningText,
                    pendingCountdown = if (uiState.showPending) uiState.pendingCountdownText else null
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ArmButton(
                        armed = uiState.armed,
                        onToggle = onToggleArmed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
