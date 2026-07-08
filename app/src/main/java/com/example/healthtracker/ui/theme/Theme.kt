package com.example.healthtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MinionYellowDark,
    onPrimary = MinionOnSurface,
    primaryContainer = MinionYellowContainer,
    secondary = MinionBlue,
    surface = MinionSurface,
    onSurface = MinionOnSurface,
    error = Red400
)

@Composable
fun HealthTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
