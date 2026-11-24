package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition

/** Work 10 s, rest 5 s. */
val shortDefinition = TimerDefinition(
    blocks = listOf(
        Block(
            steps = listOf(
                Step(name = "Work", duration = 10, type = StepType.WorkA),
                Step(name = "Rest", duration = 5, type = StepType.Rest),
            ),
        ),
    ),
)

fun request(
    definition: TimerDefinition = shortDefinition,
    options: WorkoutOptions = WorkoutOptions(),
) = WorkoutRequest(definition, "Test timer", options, startedAtEpochMillis = 1_700_000_000_000)

fun WorkoutUiState.active(): WorkoutUiState.Active = this as WorkoutUiState.Active

fun WorkoutUiState.finished(): WorkoutUiState.Finished = this as WorkoutUiState.Finished
