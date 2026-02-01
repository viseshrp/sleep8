package com.sleep8.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sleep8.ui.components.ArmButton
import com.sleep8.ui.components.StatusCard
import com.sleep8.ui.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0B1020),
            Color(0xFF101C2E),
            Color(0xFF12283A)
        )
    )

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sleep8",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFFE9EEF7)
                )
                Text(
                    text = "Auto-arm sleep, set a real alarm.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB8C3D6)
                )
            }

            StatusCard(
                status = uiState.statusText,
                armedUntil = uiState.armedUntilText,
                lastScreenOff = uiState.lastScreenOffText,
                pendingCountdown = if (uiState.showPending) uiState.pendingCountdownText else null
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ArmButton(
                    armed = uiState.armed,
                    onToggle = { viewModel.toggleArmed() },
                    modifier = Modifier.fillMaxWidth()
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick actions",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFB8C3D6)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2C43))
                        ) {
                            Text(text = "Settings", color = Color(0xFFE9EEF7))
                        }
                    }
                }
            }
        }
    }
}
