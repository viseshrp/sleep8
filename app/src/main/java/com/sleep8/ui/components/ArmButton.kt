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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ArmButton(
    armed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (armed) Color(0xFFD14F3C) else Color(0xFF3F7AE0)
    val label = if (armed) "Disarm" else "Arm Tonight"
    val sub = if (armed) "Stop monitoring" else "Start monitoring for tonight"
    Button(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(text = sub, style = MaterialTheme.typography.labelMedium, color = Color(0xFFE8EEF9))
        }
    }
}
