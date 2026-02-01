package com.sleep8.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
    pendingCountdown: String?
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Status: $status", style = MaterialTheme.typography.titleMedium)
            if (armedUntil.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Armed until: $armedUntil")
            }
            if (lastScreenOff.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Last screen off: $lastScreenOff")
            }
            if (!pendingCountdown.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Confirming: $pendingCountdown")
            }
        }
    }
}
