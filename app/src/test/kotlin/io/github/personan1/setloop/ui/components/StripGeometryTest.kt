package io.github.personan1.setloop.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StripGeometryTest {
    @Test
    fun `segments get the minimum width in dp at any density`() {
        // One 1 s step beside a 1000 s step on a 300 dp strip.
        for (density in listOf(1f, 2.625f, 3.5f)) {
            val rects = layoutStrip(listOf(1, 1000), widthPx = 300 * density, density = density)
            assertTrue(rects[0].width / density >= 3f - 0.001f, "density $density: ${rects[0]}")
        }
    }

    @Test
    fun `segments are separated by one dp and fill the width`() {
        val density = 2f
        val rects = layoutStrip(listOf(10, 10, 10), widthPx = 200 * density, density = density)
        assertEquals(3, rects.size)
        assertEquals(1f * density, rects[1].left - (rects[0].left + rects[0].width), 0.001f)
        val last = rects.last()
        assertEquals(200f * density, last.left + last.width, 0.01f)
    }

    @Test
    fun `empty input and zero width give no segments`() {
        assertTrue(layoutStrip(emptyList(), 100f, 2f).isEmpty())
        assertTrue(layoutStrip(listOf(5), 0f, 2f).isEmpty())
        assertTrue(layoutStrip(listOf(5), 100f, 0f).isEmpty())
    }
}
