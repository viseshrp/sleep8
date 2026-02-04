package com.sleep8.ui.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListContent(
    items: List<AlarmListItem>,
    updatingIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Alarm") },
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
        if (items.isEmpty()) {
            Text(
                text = "No alarms yet.",
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
                    .padding(paddingValues)
                    .testTag("alarm-list"),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    AlarmRow(
                        item = item,
                        isUpdating = updatingIds.contains(item.id),
                        onToggle = { enabled -> onToggle(item.id, enabled) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
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
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(
                text = item.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = item.enabled,
                onCheckedChange = onToggle,
                enabled = item.toggleEnabled && !isUpdating,
                modifier = Modifier.testTag("alarm-toggle-${item.id}")
            )
        }
    )
}
