package com.anb.git.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = AppWhite,
    primaryContainer = PaleBlue,
    onPrimaryContainer = RoyalBlueDark,
    secondary = RoyalBlueDark,
    background = AppWhite,
    surface = AppWhite,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun AnbGitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
