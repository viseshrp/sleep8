package com.sleep8.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5C8FF),
    onPrimary = Color(0xFF002E63),
    primaryContainer = Color(0xFF19487E),
    onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFFB7C9E6),
    onSecondary = Color(0xFF203248),
    surface = Color(0xFF10161F),
    surfaceContainer = Color(0xFF1A2230),
    surfaceContainerHigh = Color(0xFF202A3A),
    onSurface = Color(0xFFE5EAF3),
    onSurfaceVariant = Color(0xFFB7C2D4),
    outline = Color(0xFF8A96A8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C67B7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF4D6078),
    onSecondary = Color.White,
    surface = Color(0xFFF6F8FC),
    surfaceContainer = Color(0xFFE9EEF7),
    surfaceContainerHigh = Color(0xFFDDE5F3),
    onSurface = Color(0xFF1A1D24),
    onSurfaceVariant = Color(0xFF4B5565),
    outline = Color(0xFF707B8B),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun Sleep8Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}
