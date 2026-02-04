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
    primary = Color(0xFF1F4F8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9DBFA),
    onPrimaryContainer = Color(0xFF0C2C57),
    secondary = Color(0xFF314B69),
    onSecondary = Color.White,
    background = Color(0xFFEFF3F8),
    onBackground = Color(0xFF111923),
    surface = Color(0xFFEFF3F8),
    surfaceContainer = Color(0xFFE0E8F2),
    surfaceContainerHigh = Color(0xFFD3DEEC),
    onSurface = Color(0xFF111923),
    onSurfaceVariant = Color(0xFF2E3C4E),
    outline = Color(0xFF58677A),
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
