package io.github.personan1.setloop.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** The iOS AccentColor (#E8452C), kept in both themes: it is the brand and the default "Work A" color. */
val SetLoopAccent = Color(0xFFE8452C)

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = SetLoopAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD2),
    onPrimaryContainer = Color(0xFF410600),
    secondary = Color(0xFF5B6470),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = Color(0xFF1E8E52),
    onTertiary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE5E5EA),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFD1D1D6),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = SetLoopAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8A1F0C),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFFA9B1BC),
    onSecondary = Color(0xFF1C1C1E),
    secondaryContainer = Color(0xFF3A3A3C),
    onSecondaryContainer = Color(0xFFF2F2F7),
    tertiary = Color(0xFF5FD08A),
    onTertiary = Color(0xFF00210E),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF3A3A3C),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF410002),
)
