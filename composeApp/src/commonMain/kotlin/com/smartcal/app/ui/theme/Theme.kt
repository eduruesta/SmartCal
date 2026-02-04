package com.smartcal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Custom colors with the requested primary #14be82 (green)
private val CalendarGreen = Color(0xFF14be82)
private val CalendarGreenVariant = Color(0xFF0FA372) // Slightly darker variant
private val CalendarGreenLight = Color(0xFF4FD6A7) // Slightly lighter variant

private val LightColorScheme = lightColorScheme(
    primary = CalendarGreen,
    onPrimary = Color.White,
    primaryContainer = CalendarGreenLight,
    onPrimaryContainer = Color(0xFF003826),
    secondary = CalendarGreenVariant,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F5E6),
    onSecondaryContainer = Color(0xFF0B4A34),
    tertiary = Color(0xFF6750A4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21005D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    inverseOnSurface = Color(0xFFF4EFF4),
    inverseSurface = Color(0xFF313033),
    inversePrimary = CalendarGreenLight,
    surfaceTint = CalendarGreen,
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = CalendarGreenLight,
    onPrimary = Color(0xFF003826),
    primaryContainer = CalendarGreen,
    onPrimaryContainer = Color.White,
    secondary = CalendarGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0B4A34),
    onSecondaryContainer = Color(0xFFD0F5E6),
    tertiary = Color(0xFFCFBCFF),
    onTertiary = Color(0xFF361D73),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    inverseOnSurface = Color(0xFF1C1B1F),
    inverseSurface = Color(0xFFE6E1E5),
    inversePrimary = CalendarGreen,
    surfaceTint = CalendarGreenLight,
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000)
)

@Composable
fun CalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}