package com.sorin.radiotuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RadioColorScheme = darkColorScheme(
    background = Graphite900,
    surface = Graphite800,
    primary = Amber,
    secondary = LedGreen,
    onBackground = Cream,
    onSurface = Cream,
)

@Composable
fun RadioTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RadioColorScheme,
        typography = RadioTypography,
        content = content
    )
}
