package com.example.kivo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = KivoPurpleMain,
    secondary = KivoPurpleElectric,
    tertiary = KivoPink,
    background = KivoBlack,
    surface = KivoSurface1,
    onPrimary = KivoTextPrimary,
    onSecondary = KivoTextPrimary,
    onTertiary = KivoTextPrimary,
    onBackground = KivoTextPrimary,
    onSurface = KivoTextPrimary,
    surfaceVariant = KivoSurface2,
    onSurfaceVariant = KivoTextSecondary,
    outline = KivoBorder
)

// Kivo is Dark-only by design in this prompt, but keeping a fallback
private val LightColorScheme = lightColorScheme(
    primary = KivoPurpleMain,
    background = KivoTextPrimary,
    surface = KivoTextPrimary,
    onBackground = KivoBlack,
    onSurface = KivoBlack
)

@Composable
fun KIVOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
