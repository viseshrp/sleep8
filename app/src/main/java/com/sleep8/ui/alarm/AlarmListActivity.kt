package com.sleep8.ui.alarm

import android.os.Bundle
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleep8.ui.theme.Sleep8Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmListActivity : ComponentActivity() {

    private val viewModel: AlarmListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Sleep8Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmListScreen(viewModel = viewModel, onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun AlarmListScreen(
    viewModel: AlarmListViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    AlarmListContent(
        items = uiState.items,
        updatingIds = uiState.updatingIds,
        onToggle = viewModel::onToggle,
        onBack = onBack
    )
}

@Composable
fun AlarmListContent(
    items: List<AlarmListItem>,
    updatingIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
    onBack: () -> Unit
) {
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
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFE9EEF7)
                )
            }
            Text(
                text = "Alarm",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFE9EEF7)
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "No alarms yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8C3D6)
            )
        } else {
            LazyColumn(
                modifier = Modifier.testTag("alarm-list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    AlarmRow(
                        item = item,
                        isUpdating = updatingIds.contains(item.id),
                        onToggle = { enabled -> onToggle(item.id, enabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    item: AlarmListItem,
    isUpdating: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1AFFFFFF), shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE9EEF7)
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF93A4BC)
            )
        }
        Switch(
            checked = item.enabled,
            onCheckedChange = onToggle,
            enabled = item.toggleEnabled && !isUpdating,
            modifier = Modifier.testTag("alarm-toggle-${item.id}")
        )
    }
    Spacer(modifier = Modifier.height(2.dp))
}
