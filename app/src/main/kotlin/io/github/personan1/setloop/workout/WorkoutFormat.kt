package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.PlannedStep
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Duration.Companion.seconds

object WorkoutFormat {
    /** 0:05, 4:52, 12:00. Rounds up, so a running countdown never shows 0:00 before the step ends. */
    fun time(duration: Duration): String {
        val total = ceil(duration.toDouble(DurationUnit.SECONDS)).toLong().coerceAtLeast(0)
        return "%d:%02d".format(total / 60, total % 60)
    }

    fun time(seconds: Int): String = time(seconds.seconds)

    /** "Cycle 2/3 - Set 1/4"; parts that don't apply are omitted. */
    fun position(
        position: PlannedStep.Position,
        cycle: (Int, Int) -> String,
        set: (Int, Int) -> String,
    ): String = listOfNotNull(
        position.cycle?.let { current ->
            position.cycleCount?.takeIf { it > 1 }?.let { cycle(current, it) }
        },
        position.set?.takeIf { position.setCount > 1 }?.let { set(it, position.setCount) },
    ).joinToString(SEPARATOR)

    const val SEPARATOR = " \u00B7 "
}
