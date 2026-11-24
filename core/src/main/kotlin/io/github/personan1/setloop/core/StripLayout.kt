package io.github.personan1.setloop.core

import kotlin.math.max

/** Lays out a colored strip of steps: widths follow durations, each segment at least `minWidth` (DESIGN V4). */
object StripLayout {
    data class Segment(val x: Double, val width: Double)

    /** When the segments cannot all get `minWidth` and the gaps, they are packed proportionally without gaps. */
    fun segments(
        durations: List<Int>,
        width: Double,
        minWidth: Double = 3.0,
        spacing: Double = 1.0,
    ): List<Segment> {
        if (durations.isEmpty() || width <= 0) return emptyList()
        val weights = durations.map { max(it, 0).toDouble() }
        val count = durations.size.toDouble()
        val fits = width - spacing * (count - 1) >= minWidth * count

        val gap = if (fits) spacing else 0.0
        val available = width - gap * (count - 1)
        val widths = if (fits) clamped(weights, available, minWidth) else proportional(weights, available)

        var x = 0.0
        return widths.map { w ->
            Segment(x, w).also { x += w + gap }
        }
    }

    private fun proportional(weights: List<Double>, width: Double): List<Double> {
        val total = weights.sum()
        if (total <= 0) return weights.map { width / weights.size }
        return weights.map { width * it / total }
    }

    /** Pins too-narrow segments to `minWidth` and shares the rest among the others, until nothing changes. */
    private fun clamped(weights: List<Double>, width: Double, minWidth: Double): List<Double> {
        val pinned = mutableSetOf<Int>()
        while (true) {
            val free = weights.indices.filter { it !in pinned }
            val shared = proportional(free.map { weights[it] }, width - minWidth * pinned.size)
            val narrow = free.zip(shared).filter { it.second < minWidth }.map { it.first }
            if (narrow.isEmpty()) {
                val result = MutableList(weights.size) { minWidth }
                free.zip(shared).forEach { (index, w) -> result[index] = w }
                return result
            }
            pinned.addAll(narrow)
        }
    }
}
