package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.RGB
import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.core.StepColors
import io.github.personan1.setloop.core.StepType
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutOptionsTest {
    @Test
    fun `defaults match the iOS settings`() {
        val options = WorkoutOptions()
        assertEquals(SoundSet.Glass, options.soundSet)
        assertEquals(true, options.soundsEnabled)
        assertEquals(false, options.voicePrompts)
        assertEquals(true, options.vibration)
        assertEquals(true, options.ignoreSilentMode)
        assertEquals(true, options.showOnLockScreen)
        assertEquals(true, options.pauseOnInterruption)
        assertEquals(StepColors.DEFAULT, options.stepColors)
    }

    @Test
    fun `round-trips through json`() {
        val options = WorkoutOptions(
            soundSet = SoundSet.Marimba,
            voicePrompts = true,
            stepColors = StepColors(StepColors.DEFAULT.colors + (StepType.Rest to RGB(0x0E7C9E))),
        )
        assertEquals(options, WorkoutOptions.fromJson(options.toJson()))
    }

    @Test
    fun `missing keys fall back to defaults`() {
        assertEquals(WorkoutOptions(), WorkoutOptions.fromJson("{}"))
    }
}
