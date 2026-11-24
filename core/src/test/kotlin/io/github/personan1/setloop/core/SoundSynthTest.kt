package io.github.personan1.setloop.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoundSynthTest {
    @Test
    fun everyCueIsAudibleAndNormalized() {
        for (set in SoundSet.entries) {
            for (cue in SoundCue.entries) {
                val samples = SoundSynth.samples(cue, set)
                val level = samples.maxOfOrNull { abs(it) } ?: 0f
                assertTrue(abs(level - SoundSynth.PEAK) < 0.001, "$set $cue")
                // Silent edges: no clicks at start and end.
                assertTrue(abs(samples.firstOrNull() ?: 1f) < 0.01)
                assertTrue(abs(samples.lastOrNull() ?: 1f) < 0.01)
            }
        }
    }

    @Test
    fun countdownIsShortAndFinishIsLongest() {
        for (set in SoundSet.entries) {
            fun length(cue: SoundCue) = SoundSynth.samples(cue, set).size
            assertTrue(length(SoundCue.Countdown) < length(SoundCue.Work), "$set")
            assertTrue(length(SoundCue.Finish) > length(SoundCue.Work), "$set")
        }
    }

    @Test
    fun stepTypesMapToCues() {
        assertEquals(SoundCue.Work, SoundCue.from(StepType.WorkB))
        assertEquals(SoundCue.Rest, SoundCue.from(StepType.Rest))
        assertEquals(SoundCue.Other, SoundCue.from(StepType.Cooldown))
    }
}
