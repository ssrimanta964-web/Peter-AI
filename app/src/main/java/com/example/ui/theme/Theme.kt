package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PeterColorScheme = darkColorScheme(
    primary = SophisticatedCyan,
    onPrimary = SophisticatedBlack,
    primaryContainer = SophisticatedSurface,
    onPrimaryContainer = SophisticatedCyan,
    secondary = SophisticatedCyanMedium,
    onSecondary = TextWhite,
    secondaryContainer = SophisticatedCard,
    onSecondaryContainer = TextPrimary,
    tertiary = SophisticatedCyan,
    background = SophisticatedBlack,
    onBackground = TextPrimary,
    surface = SophisticatedPanel,
    onSurface = TextPrimary,
    surfaceVariant = SophisticatedCard,
    onSurfaceVariant = TextSecondary,
    outline = SophisticatedBorder,
    outlineVariant = SophisticatedBorderLight,
    error = StatusRed,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Sophisticated Dark is default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = PeterColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SophisticatedBlack.toArgb()
                window.navigationBarColor = SophisticatedPanel.toArgb()
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = false
                windowInsetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
