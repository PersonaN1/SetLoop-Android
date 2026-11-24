package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.PlannedStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkoutFormatTest {
    @Test
    fun `time rounds up`() {
        assertEquals("0:05", WorkoutFormat.time(4_100.milliseconds))
        assertEquals("4:52", WorkoutFormat.time(292.seconds))
        assertEquals("12:00", WorkoutFormat.time(720))
        assertEquals("0:00", WorkoutFormat.time((-1).seconds))
    }

    @Test
    fun `position omits parts that do not apply`() {
        fun position(cycle: Int?, cycleCount: Int?, set: Int?, setCount: Int) =
            WorkoutFormat.position(
                PlannedStep.Position(set = set, setCount = setCount, block = 1, blockCount = 1, cycle = cycle, cycleCount = cycleCount),
                cycle = { a, b -> "Cycle $a/$b" },
                set = { a, b -> "Set $a/$b" },
            )
        assertEquals("Cycle 2/3${WorkoutFormat.SEPARATOR}Set 1/4", position(2, 3, 1, 4))
        assertEquals("Cycle 2/3", position(2, 3, 1, 1))
        assertEquals("Set 1/4", position(1, 1, 1, 4))
        assertEquals("", position(null, null, null, 1))
    }
}
