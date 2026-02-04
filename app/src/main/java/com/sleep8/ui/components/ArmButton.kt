package com.sleep8.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ArmButton(
    armed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (armed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val content = MaterialTheme.colorScheme.onSurface
    val label = if (armed) "Disarm" else "Arm Tonight"
    val sub = if (armed) "Stop monitoring" else "Start monitoring for tonight"
    FilledTonalButton(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge)
            Text(
                text = sub,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
    }
}
