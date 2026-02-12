package com.satwik.aimemory.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AiMemoryColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = DarkBackground,
    primaryContainer = AccentTealDim,
    onPrimaryContainer = TextPrimary,
    secondary = WarmAmber,
    onSecondary = DarkBackground,
    secondaryContainer = WarmAmberDim,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentCyan,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary,
    outline = TextMuted
)

@Composable
fun AiMemoryTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AiMemoryColorScheme,
        typography = AiMemoryTypography,
        content = content
    )
}
