package io.github.personan1.setloop.workout

import android.content.Context
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.PlannedStep
import io.github.personan1.setloop.core.cappedName

/** Localized workout strings shared by the notification and the screen. */
class WorkoutTexts(private val context: Context) {
    fun position(step: PlannedStep): String = WorkoutFormat.position(
        step.position,
        cycle = { current, count -> context.getString(R.string.workout_cycle, current, count) },
        set = { current, count -> context.getString(R.string.workout_set, current, count) },
    )

    fun next(step: PlannedStep): String =
        context.getString(R.string.workout_next, step.step.name.cappedName, WorkoutFormat.time(step.duration))
}
