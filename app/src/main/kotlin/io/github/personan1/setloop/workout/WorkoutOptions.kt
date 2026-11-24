package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.core.StepColors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Settings that affect a running workout. Defaults equal the iOS `AppSettings` defaults (SPEC D5-D7).
 * Passed to the service at start and kept in the process-death snapshot.
 */
@Serializable
data class WorkoutOptions(
    val soundSet: SoundSet = SoundSet.Glass,
    /** iOS "Off" sound set. */
    val soundsEnabled: Boolean = true,
    val voicePrompts: Boolean = false,
    val vibration: Boolean = true,
    val ignoreSilentMode: Boolean = true,
    val showOnLockScreen: Boolean = true,
    /** Pause on a call or another audio interruption (SPEC D6). */
    val pauseOnInterruption: Boolean = true,
    val stepColors: StepColors = StepColors.DEFAULT,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): WorkoutOptions = json.decodeFromString(serializer(), text)
    }
}
