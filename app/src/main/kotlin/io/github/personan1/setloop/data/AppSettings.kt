package io.github.personan1.setloop.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.core.StepColors
import io.github.personan1.setloop.core.TimerDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme { System, Light, Dark }

/** Immutable snapshot of the Settings tab (SPEC D22). Defaults are from SPEC D5-D7. */
data class AppSettingsValues(
    val pauseOnInterruption: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val keepScreenOn: Boolean = true,
    val vibration: Boolean = true,
    val voicePrompts: Boolean = false,
    val ignoreSilentMode: Boolean = true,
    /** Null is "Off". */
    val soundSet: SoundSet? = SoundSet.Glass,
    val theme: AppTheme = AppTheme.System,
    val stepColors: StepColors = StepColors.DEFAULT,
)

/** User preferences on Preferences DataStore, mirroring iOS `AppSettings`. */
class AppSettings(private val dataStore: DataStore<Preferences>) {
    val values: Flow<AppSettingsValues> = dataStore.data.map { it.toValues() }

    suspend fun setPauseOnInterruption(value: Boolean) = set(Keys.pauseOnInterruption, value)

    suspend fun setShowOnLockScreen(value: Boolean) = set(Keys.showOnLockScreen, value)

    suspend fun setKeepScreenOn(value: Boolean) = set(Keys.keepScreenOn, value)

    suspend fun setVibration(value: Boolean) = set(Keys.vibration, value)

    suspend fun setVoicePrompts(value: Boolean) = set(Keys.voicePrompts, value)

    suspend fun setIgnoreSilentMode(value: Boolean) = set(Keys.ignoreSilentMode, value)

    suspend fun setSoundSet(value: SoundSet?) = set(Keys.soundSet, value?.raw ?: OFF)

    suspend fun setTheme(value: AppTheme) = set(Keys.theme, value.name.lowercase())

    suspend fun setStepColors(value: StepColors) =
        set(Keys.stepColors, TimerDefinition.json.encodeToString(StepColors.serializer(), value))

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toValues(): AppSettingsValues {
        val defaults = AppSettingsValues()
        val storedSet = this[Keys.soundSet]
        return AppSettingsValues(
            pauseOnInterruption = this[Keys.pauseOnInterruption] ?: defaults.pauseOnInterruption,
            showOnLockScreen = this[Keys.showOnLockScreen] ?: defaults.showOnLockScreen,
            keepScreenOn = this[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            vibration = this[Keys.vibration] ?: defaults.vibration,
            voicePrompts = this[Keys.voicePrompts] ?: defaults.voicePrompts,
            ignoreSilentMode = this[Keys.ignoreSilentMode] ?: defaults.ignoreSilentMode,
            // A set that no longer exists falls back to the default rather than to Off.
            soundSet = if (storedSet == OFF) null else SoundSet.entries.firstOrNull { it.raw == storedSet }
                ?: defaults.soundSet,
            theme = AppTheme.entries.firstOrNull { it.name.lowercase() == this[Keys.theme] } ?: defaults.theme,
            stepColors = this[Keys.stepColors]?.let(::decodeStepColors) ?: defaults.stepColors,
        )
    }

    private fun decodeStepColors(text: String): StepColors? = try {
        TimerDefinition.json.decodeFromString(StepColors.serializer(), text)
    } catch (_: IllegalArgumentException) {
        null
    }

    private object Keys {
        val pauseOnInterruption = booleanPreferencesKey("pauseOnInterruption")
        val showOnLockScreen = booleanPreferencesKey("showOnLockScreen")
        val keepScreenOn = booleanPreferencesKey("keepScreenOn")
        val vibration = booleanPreferencesKey("vibration")
        val voicePrompts = booleanPreferencesKey("voicePrompts")
        val ignoreSilentMode = booleanPreferencesKey("ignoreSilentMode")
        val soundSet = stringPreferencesKey("soundSet")
        val theme = stringPreferencesKey("theme")
        val stepColors = stringPreferencesKey("stepColors")
    }

    private companion object {
        const val OFF = "off"
    }
}

private val SoundSet.raw: String get() = name.lowercase()
