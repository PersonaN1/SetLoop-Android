package io.github.personan1.setloop.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.personan1.setloop.core.StepRecord
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.core.WorkoutSummary
import io.github.personan1.setloop.core.completedCount
import io.github.personan1.setloop.core.time
import kotlinx.serialization.builtins.ListSerializer

/**
 * A finished workout saved from the summary screen (SPEC D10). Independent of the timer it came from.
 * Mirrors iOS `WorkoutRecord`.
 */
@Entity(tableName = "workout_record", indices = [Index("startedAt")])
data class WorkoutRecordEntity(
    @PrimaryKey val id: String,
    /** Epoch millis. */
    val startedAt: Long,
    val name: String,
    val note: String,
    /** Active time without pauses, seconds. */
    val duration: Int,
    /** `complete` or `partial`, see [status]. */
    val statusRaw: String,
    val workTime: Int,
    val restTime: Int,
    val otherTime: Int,
    val completedSteps: Int,
    val totalSteps: Int,
    /** JSON array of `StepRecord`. */
    val stepsData: String,
    /** Snapshot of the timer structure for "Repeat"; editing or deleting the timer does not touch it. */
    val definitionData: String,
) {
    val status: WorkoutStatus
        get() = WorkoutStatus.entries.firstOrNull { it.raw == statusRaw } ?: WorkoutStatus.Complete

    /** Null when the snapshot can't be decoded. */
    val definition: TimerDefinition?
        get() = try {
            TimerDefinition.fromJson(definitionData)
        } catch (_: IllegalArgumentException) {
            null
        }

    val steps: List<StepRecord>
        get() = try {
            TimerDefinition.json.decodeFromString(stepsSerializer, stepsData)
        } catch (_: IllegalArgumentException) {
            emptyList()
        }

    companion object {
        private val stepsSerializer = ListSerializer(StepRecord.serializer())

        fun create(
            id: String,
            name: String,
            note: String,
            startedAt: Long,
            summary: WorkoutSummary,
            definition: TimerDefinition,
        ): WorkoutRecordEntity = WorkoutRecordEntity(
            id = id,
            startedAt = startedAt,
            name = name,
            note = note,
            duration = summary.duration,
            statusRaw = summary.status.raw,
            workTime = summary.steps.time(StepType.Category.Work),
            restTime = summary.steps.time(StepType.Category.Rest),
            otherTime = summary.steps.time(StepType.Category.Other),
            completedSteps = summary.steps.completedCount,
            totalSteps = summary.totalSteps,
            stepsData = TimerDefinition.json.encodeToString(stepsSerializer, summary.steps),
            definitionData = definition.toJson(),
        )
    }
}

private val WorkoutStatus.raw: String get() = name.lowercase()
