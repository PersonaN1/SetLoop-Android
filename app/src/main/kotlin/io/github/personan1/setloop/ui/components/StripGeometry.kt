package io.github.personan1.setloop.ui.components

import io.github.personan1.setloop.core.StripLayout

/** A strip segment in pixels. */
data class StripRect(val left: Float, val width: Float)

internal const val STRIP_MIN_SEGMENT_DP = 3.0
internal const val STRIP_SPACING_DP = 1.0
internal const val STRIP_SEGMENT_RADIUS_DP = 2f

/**
 * Lays out segments in dp, as the design states the minimum width (DESIGN V4), then maps to pixels,
 * so the 3 dp minimum holds at every screen density.
 */
fun layoutStrip(durations: List<Int>, widthPx: Float, density: Float): List<StripRect> {
    if (density <= 0f) return emptyList()
    val segments = StripLayout.segments(
        durations = durations,
        width = widthPx / density.toDouble(),
        minWidth = STRIP_MIN_SEGMENT_DP,
        spacing = STRIP_SPACING_DP,
    )
    return segments.map { StripRect((it.x * density).toFloat(), (it.width * density).toFloat()) }
}
