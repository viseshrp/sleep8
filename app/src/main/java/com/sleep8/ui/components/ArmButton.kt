package com.sleep8.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ArmButton(armed: Boolean, onToggle: () -> Unit) {
    Button(onClick = onToggle) {
        Text(text = if (armed) "Disarm" else "Arm Tonight")
    }
}
