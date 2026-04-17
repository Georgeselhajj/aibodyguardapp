package com.aibodyguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4B544),
    onPrimary = Color(0xFF09131B),
    secondary = Color(0xFF5AD9A8),
    onSecondary = Color(0xFF09131B),
    tertiary = Color(0xFF7CCBFF),
    background = Color(0xFF09131B),
    onBackground = Color(0xFFF4F7F9),
    surface = Color(0xFF132431),
    onSurface = Color(0xFFF4F7F9),
    surfaceVariant = Color(0xFF1C3344),
    onSurfaceVariant = Color(0xFFA8BAC7),
    outline = Color(0xFF294052),
    error = Color(0xFFFF7676)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF935F00),
    onPrimary = Color.White,
    secondary = Color(0xFF006A4E),
    onSecondary = Color.White,
    tertiary = Color(0xFF006497),
    background = Color(0xFFF4F7F9),
    onBackground = Color(0xFF09131B),
    surface = Color.White,
    onSurface = Color(0xFF09131B),
    surfaceVariant = Color(0xFFE4EDF3),
    onSurfaceVariant = Color(0xFF415462),
    outline = Color(0xFF738694),
    error = Color(0xFFBA1A1A)
)

@Composable
fun AIBodyguardTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
