package io.github.personan1.setloop.data

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.totalDuration

/** A saved timer: list fields plus the whole structure as one JSON value (TECH T5). Mirrors iOS `SavedTimer`. */
@Entity(tableName = "saved_timer")
data class SavedTimerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortIndex: Int,
    /** Epoch millis. */
    val createdAt: Long,
    /** Epoch millis. */
    val updatedAt: Long,
    /** Seconds, cached from the definition for the list. */
    val totalDuration: Int,
    /** `TimerDefinition` JSON, the same format as iOS. */
    val definitionData: String,
) {
    /** A damaged timer shows up empty instead of stopping the app, as on iOS. */
    val definition: TimerDefinition
        get() = try {
            TimerDefinition.fromJson(definitionData)
        } catch (error: IllegalArgumentException) {
            Log.e(LOG_TAG, "Timer definition can't be decoded", error)
            TimerDefinition(blocks = emptyList())
        }

    fun withDefinition(name: String, definition: TimerDefinition, now: Long): SavedTimerEntity = copy(
        name = name,
        updatedAt = now,
        totalDuration = definition.totalDuration.toInt(),
        definitionData = definition.toJson(),
    )

    companion object {
        private const val LOG_TAG = "SavedTimerEntity"

        fun create(id: String, name: String, definition: TimerDefinition, sortIndex: Int, now: Long) =
            SavedTimerEntity(
                id = id,
                name = name,
                sortIndex = sortIndex,
                createdAt = now,
                updatedAt = now,
                totalDuration = definition.totalDuration.toInt(),
                definitionData = definition.toJson(),
            )
    }
}
