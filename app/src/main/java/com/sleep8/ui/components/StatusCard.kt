package com.sleep8.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StatusCard(
    status: String,
    armedUntil: String,
    lastScreenOff: String,
    latestAlarmText: String,
    latestAlarmSubtitle: String,
    systemNextAlarmText: String,
    notificationWarningText: String,
    pendingCountdown: String?
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFE9EEF7)
            )
            if (armedUntil.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Armed until", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
                    Text(text = armedUntil, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE9EEF7))
                }
            }
            if (lastScreenOff.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Last screen off", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
                    Text(text = lastScreenOff, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE9EEF7))
                }
            }
            if (latestAlarmText.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Latest alarm", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
                    Text(text = latestAlarmText, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE9EEF7))
                }
                if (latestAlarmSubtitle.isNotBlank()) {
                    Text(text = latestAlarmSubtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF93A4BC))
                }
            }
            if (systemNextAlarmText.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "System next alarm", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C3D6))
                    Text(text = systemNextAlarmText, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE9EEF7))
                }
            }
            if (notificationWarningText.isNotBlank()) {
                Text(
                    text = notificationWarningText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFBBF24)
                )
            }
            if (!pendingCountdown.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confirming • $pendingCountdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF93C5FD)
                )
            }
        }
    }
}
