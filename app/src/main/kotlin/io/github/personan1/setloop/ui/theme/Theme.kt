package io.github.personan1.setloop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.github.personan1.setloop.data.AppTheme

@Composable
fun isDarkTheme(theme: AppTheme): Boolean = when (theme) {
    AppTheme.System -> isSystemInDarkTheme()
    AppTheme.Light -> false
    AppTheme.Dark -> true
}

/** Brand theme without dynamic color: light or dark by the `theme` setting. */
@Composable
fun SetLoopTheme(theme: AppTheme = AppTheme.System, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDarkTheme(theme)) DarkColorScheme else LightColorScheme,
        typography = SetLoopTypography,
        content = content,
    )
}
