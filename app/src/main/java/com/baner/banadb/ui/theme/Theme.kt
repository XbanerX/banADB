package com.baner.banadb.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandCyan,
    onPrimary = DarkBackground,
    primaryContainer = BrandCyanDark,
    onPrimaryContainer = BrandCyanLight,
    secondary = BrandCyanLight,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = BrandCyanLight,
    tertiary = NeonEmerald,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = DarkTextSecondary,
    error = StatusError,
    onError = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = BrandCyanDeep,
    onPrimary = LightSurface,
    primaryContainer = BrandCyanLight,
    onPrimaryContainer = LightTextPrimary,
    secondary = BrandCyanDark,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceCard,
    onSecondaryContainer = LightTextPrimary,
    tertiary = NeonEmerald,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceCard,
    onSurfaceVariant = LightTextSecondary,
    error = StatusError,
    onError = LightSurface
)

@Composable
fun BanaAdbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkBanaPalette else LightBanaPalette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.surface.toArgb()
                window.navigationBarColor = palette.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalBanaPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
