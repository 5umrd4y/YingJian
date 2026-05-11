package com.yingjian.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    surface = Surface,
    onSurface = OnSurface,
    background = Background,
    onBackground = OnBackground,
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    outlineVariant = OutlineVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceVariant = SurfaceVariant,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    surface = Color(0xFF1b1c1a),
    onSurface = Color(0xFFe4e2df),
    background = Color(0xFF141312),
    onBackground = Color(0xFFe4e2df),
    primary = Color(0xFFd5c4b0),
    onPrimary = Color(0xFF3a2f22),
    primaryContainer = Color(0xFF504535),
    onPrimaryContainer = Color(0xFFf2e0cb),
    secondaryContainer = Color(0xFF4e453c),
    onSecondaryContainer = Color(0xFFede0d3),
    outlineVariant = Color(0xFF4c463e),
    surfaceContainerLowest = Color(0xFF1b1c1a),
    surfaceContainerLow = Color(0xFF232422),
    surfaceContainer = Color(0xFF2a2b28),
    surfaceVariant = Color(0xFF3a3937),
    error = Color(0xFFf2b8b5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8c1d18)
)

@Composable
fun YingJianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YingJianTypography,
        content = content
    )
}
