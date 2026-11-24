package io.github.personan1.setloop.core

import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit

/** What the summary screen and history show about a finished workout (SPEC D10, DESIGN V5). */
data class WorkoutSummary(
    val status: WorkoutStatus,
    /** Active time without pauses, whole seconds. */
    val duration: Int,
    /** Steps actually reached, with outcomes. */
    val steps: List<StepRecord>,
    /** Steps in the plan. A final rest removed by "Skip last rest" is not part of the plan (SPEC D10). */
    val totalSteps: Int,
) {
    val completedSteps: Int get() = steps.completedCount

    fun time(category: StepType.Category): Int = steps.time(category)

    companion object {
        /** Null until the workout has finished. */
        fun from(engine: WorkoutEngine, now: Duration): WorkoutSummary? {
            val phase = engine.phase as? WorkoutEngine.Phase.Finished ?: return null
            return WorkoutSummary(
                status = phase.status,
                duration = engine.activeTime(now).toDouble(DurationUnit.SECONDS).roundToInt(),
                steps = engine.log,
                totalSteps = engine.plan.steps.size,
            )
        }
    }
}

val List<StepRecord>.completedCount: Int get() = count { it.outcome == StepRecord.Outcome.Completed }

/** Time actually spent on steps of the category, including the part of a skipped or interrupted step. */
fun List<StepRecord>.time(category: StepType.Category): Int =
    filter { it.step.type.category == category }.sumOf { it.elapsed }
