package io.github.personan1.setloop.ui.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkoutScreenLogicTest {
    @Test
    fun `countdown rounds up so it shows 0 01 until the step ends`() {
        assertEquals(6, wholeSecondsLeft(5_200.milliseconds))
        assertEquals(5, wholeSecondsLeft(5.seconds))
        assertEquals(1, wholeSecondsLeft(1.milliseconds))
        assertEquals(0, wholeSecondsLeft(0.milliseconds))
        assertEquals(0, wholeSecondsLeft((-4).seconds))
    }
}
