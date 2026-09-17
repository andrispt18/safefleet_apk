package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CockpitColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF00363D),
    primaryContainer = DeepNavy,
    onPrimaryContainer = CyanAccent,
    secondary = ElectricBlue,
    onSecondary = Color(0xFF00344F),
    secondaryContainer = CockpitSurfaceVariant,
    onSecondaryContainer = ElectricBlue,
    tertiary = SafetySafe,
    onTertiary = Color(0xFF00391C),
    background = CockpitBackground,
    onBackground = TextPrimary,
    surface = CockpitSurface,
    onSurface = TextPrimary,
    surfaceVariant = CockpitSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CockpitBorder,
    error = SafetyCritical,
    onError = Color.White
)

@Composable
fun CockpitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CockpitColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for tests and previews
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CockpitTheme(content = content)
}

