package com.perceptiveus.reverie.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.perceptiveus.reverie.core.settings.AppThemePreference

private val ReverieDarkColorScheme = darkColorScheme(
    primary = ReveriePurple,
    onPrimary = Color(0xFF1A0A2E),
    primaryContainer = ReveriePurpleDim,
    onPrimaryContainer = Color.White,
    secondary = ReveriePurpleGlow,
    onSecondary = Color(0xFF1A0A2E),
    tertiary = ReveriePremiumGold,
    background = ReverieBackground,
    onBackground = ReverieOnBackground,
    surface = ReverieSurface,
    onSurface = ReverieOnBackground,
    surfaceVariant = ReverieSurfaceVariant,
    onSurfaceVariant = ReverieOnSurfaceMuted,
    outline = ReverieOutline,
)

private val ReverieLightColorScheme = lightColorScheme(
    primary = Color(0xFF6B3FA0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D4FF),
    onPrimaryContainer = Color(0xFF2A0A4E),
    secondary = Color(0xFF7B5BB5),
    background = ReverieLightBackground,
    onBackground = ReverieLightOnBackground,
    surface = ReverieLightSurface,
    onSurface = ReverieLightOnBackground,
    surfaceVariant = ReverieLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF5C5670),
    outline = Color(0xFFC8BFE0),
)

@Composable
fun ReverieTheme(
    themePreference: AppThemePreference = AppThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themePreference) {
        AppThemePreference.SYSTEM -> systemDark
        AppThemePreference.LIGHT -> false
        AppThemePreference.DARK -> true
    }

    val colorScheme = if (darkTheme) ReverieDarkColorScheme else ReverieLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
