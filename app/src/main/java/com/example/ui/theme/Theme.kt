package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ThemePrimary,
    onPrimary = ThemeOnPrimary,
    secondary = ThemeSecondary,
    onSecondary = ThemeOnSecondary,
    background = ThemeBackground,
    onBackground = ThemeOnBackground,
    surface = ThemeSurface,
    onSurface = ThemeOnSurface,
    surfaceVariant = ThemeSurfaceVariant,
    onSurfaceVariant = ThemeOnSurfaceVariant,
    outline = ThemeOutline
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    secondary = Color(0xFF7E57C2),
    onSecondary = Color.White,
    background = Color(0xFFFBF8FC),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFF0EBF5),
    onSurfaceVariant = Color(0xFF6B6577),
    outline = Color(0xFFD6CEE2)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "DARK",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

