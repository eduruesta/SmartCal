package com.smartcal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Custom colors with the requested blue #478bf8
private val CalendarBlue = Color(0xFF478bf8)
private val CalendarBlueVariant = Color(0xFF3574e6) // Slightly darker variant
private val CalendarBlueLight = Color(0xFF6ba3fa) // Slightly lighter variant

private val LightColorScheme = lightColorScheme(
    primary = CalendarBlue,
    onPrimary = Color.White,
    primaryContainer = CalendarBlueLight,
    onPrimaryContainer = Color.White,
    secondary = CalendarBlueVariant,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF1565C0),
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
    inversePrimary = Color(0xFF6ba3fa),
    surfaceTint = CalendarBlue,
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = CalendarBlueLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = CalendarBlue,
    onPrimaryContainer = Color.White,
    secondary = CalendarBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF004881),
    onSecondaryContainer = Color(0xFFD1E4FF),
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
    inversePrimary = CalendarBlue,
    surfaceTint = CalendarBlueLight,
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