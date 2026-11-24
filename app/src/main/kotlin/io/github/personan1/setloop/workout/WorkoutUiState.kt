package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.PlannedStep
import io.github.personan1.setloop.core.RGB
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.WorkoutSummary
import kotlin.time.Duration

/** What the UI shows. Time-dependent values are not ticked here: the UI derives them from [Active.remaining]. */
sealed interface WorkoutUiState {
    data object Idle : WorkoutUiState

    data class Active(
        val timerName: String,
        val step: PlannedStep,
        val next: PlannedStep?,
        val color: RGB,
        val isPaused: Boolean,
        /** Position of the current step in the plan, 0-based. */
        val stepIndex: Int,
        val stepCount: Int,
        /** End of the step on the [WorkoutClock] timeline; meaningful while running. */
        val stepEnd: Duration,
        /** Time left in the step while paused. */
        val pausedRemaining: Duration,
        /** Time left in the whole workout at the moment of this state, if it keeps running. */
        val workoutRemaining: Duration,
    ) : WorkoutUiState {
        fun remaining(now: Duration): Duration =
            if (isPaused) pausedRemaining else maxOf(stepEnd - now, Duration.ZERO)
    }

    data class Finished(val result: WorkoutResult) : WorkoutUiState
}

/** A finished workout, ready to be saved to history. */
data class WorkoutResult(
    val timerName: String,
    /** Kept so "Repeat" works after the timer is edited or deleted. */
    val definition: TimerDefinition,
    val startedAtEpochMillis: Long,
    val summary: WorkoutSummary,
)
