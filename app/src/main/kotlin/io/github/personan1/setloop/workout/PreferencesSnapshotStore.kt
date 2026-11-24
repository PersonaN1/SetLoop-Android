package io.github.personan1.setloop.workout

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.SerializationException

/** One small JSON value, rewritten on every workout command. */
class PreferencesSnapshotStore(context: Context) : SnapshotStore {
    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun read(): WorkoutSnapshot? {
        val text = preferences.getString(KEY, null) ?: return null
        return try {
            WorkoutSnapshot.fromJson(text)
        } catch (error: SerializationException) {
            // A snapshot written by an incompatible build must not crash-loop the service.
            clear()
            null
        }
    }

    override fun write(snapshot: WorkoutSnapshot) = preferences.edit { putString(KEY, snapshot.toJson()) }

    override fun clear() = preferences.edit { remove(KEY) }

    private companion object {
        const val FILE = "workout_snapshot"
        const val KEY = "snapshot"
    }
}
