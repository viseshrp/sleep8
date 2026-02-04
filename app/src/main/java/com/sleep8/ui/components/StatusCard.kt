package com.sleep8.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusCard(
    status: String,
    armedUntil: String,
    lastScreenOff: String,
    latestAlarmText: String,
    latestAlarmSubtitle: String,
    notificationWarningText: String,
    pendingCountdown: String?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.headlineSmall
            )
            if (armedUntil.isNotBlank()) {
                KeyValueRow(label = "Armed until", value = armedUntil)
            }
            if (lastScreenOff.isNotBlank()) {
                KeyValueRow(label = "Last screen off", value = lastScreenOff)
            }
            if (latestAlarmText.isNotBlank()) {
                KeyValueRow(label = "Latest alarm", value = latestAlarmText)
                if (latestAlarmSubtitle.isNotBlank()) {
                    Text(
                        text = latestAlarmSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (notificationWarningText.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = notificationWarningText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (!pendingCountdown.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confirming • $pendingCountdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
