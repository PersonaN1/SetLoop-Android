package io.github.personan1.setloop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** From this system font scale on, rows of several texts stack instead of squeezing (iOS: accessibility sizes). */
const val LARGE_FONT_SCALE = 1.3f

val isLargeFontScale: Boolean
    @Composable get() = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

/** Limits the font scale for [content] whose size is not meant to grow without bound (timer digits). */
@Composable
fun WithMaxFontScale(maxScale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val capped = if (density.fontScale > maxScale) Density(density.density, maxScale) else density
    CompositionLocalProvider(LocalDensity provides capped, content = content)
}
