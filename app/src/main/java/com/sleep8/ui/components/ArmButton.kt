package com.sleep8.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ArmButton(
    armed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (armed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
    val content = if (armed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
    val subColor = content.copy(alpha = 0.8f)
    val label = if (armed) "Disarm" else "Arm Tonight"
    val sub = if (armed) "Stop monitoring" else "Start monitoring for tonight"

    Button(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge)
            Text(
                text = sub,
                style = MaterialTheme.typography.labelMedium,
                color = subColor
            )
        }
    }
}

