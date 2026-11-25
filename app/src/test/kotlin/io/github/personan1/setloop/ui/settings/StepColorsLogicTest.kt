package io.github.personan1.setloop.ui.settings

import io.github.personan1.setloop.core.Palette
import io.github.personan1.setloop.core.RGB
import io.github.personan1.setloop.core.StepColors
import io.github.personan1.setloop.core.StepType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StepColorsLogicTest {
    private val colors = StepColors.DEFAULT
    private val green = RGB(0x1E8E52) // default Rest
    private val red = RGB(0xE8452C) // default Work A
    private val blue = RGB(0x2F6BDB)

    @Test
    fun `the rest color is blocked for every work type`() {
        listOf(StepType.WorkA, StepType.WorkB, StepType.WorkC).forEach {
            assertEquals(SwatchState.Blocked, colors.swatchState(it, green), it.name)
        }
    }

    @Test
    fun `work colors are blocked for rest`() {
        assertEquals(SwatchState.Blocked, colors.swatchState(StepType.Rest, red))
        assertEquals(SwatchState.Blocked, colors.swatchState(StepType.Rest, RGB(0x9C2BB0)))
        assertEquals(SwatchState.Blocked, colors.swatchState(StepType.Rest, RGB(0xB86200)))
    }

    @Test
    fun `prepare and cooldown may take any color`() {
        Palette.colors.forEach {
            assertTrue(colors.swatchState(StepType.Prepare, it) != SwatchState.Blocked)
            assertTrue(colors.swatchState(StepType.Cooldown, it) != SwatchState.Blocked)
        }
    }

    @Test
    fun `two work types may share a color`() {
        val picked = colors.picked(StepType.WorkB, red)

        assertEquals(red, picked[StepType.WorkB])
        assertEquals(red, picked[StepType.WorkA])
    }

    @Test
    fun `the current color shows as selected`() {
        assertEquals(SwatchState.Selected, colors.swatchState(StepType.Rest, green))
        assertEquals(SwatchState.Available, colors.swatchState(StepType.Rest, blue))
    }

    @Test
    fun `picking an allowed color changes only that type`() {
        val picked = colors.picked(StepType.WorkA, blue)

        assertEquals(blue, picked[StepType.WorkA])
        assertEquals(colors[StepType.WorkB], picked[StepType.WorkB])
        assertEquals(green, picked[StepType.Rest])
    }

    @Test
    fun `picking a blocked color changes nothing`() {
        assertSame(colors, colors.picked(StepType.WorkA, green))
        assertSame(colors, colors.picked(StepType.Rest, red))
    }

    @Test
    fun `a rest color picked first frees the old one for work`() {
        val picked = colors.picked(StepType.Rest, blue)

        assertEquals(SwatchState.Available, picked.swatchState(StepType.WorkA, green))
        assertEquals(SwatchState.Blocked, picked.swatchState(StepType.WorkA, blue))
    }

    @Test
    fun `default detection ignores missing types`() {
        assertTrue(colors.isDefault)
        assertTrue(StepColors(emptyMap()).isDefault)
        assertFalse(colors.picked(StepType.Prepare, blue).isDefault)
    }

    @Test
    fun `every palette color has its own name`() {
        val names = Palette.colors.map { paletteName(it) }

        assertEquals(Palette.colors.size, names.toSet().size)
        assertFalse(names.contains(paletteName(RGB(0x123456))))
    }
}
