package io.github.personan1.setloop.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import io.github.personan1.setloop.core.Presets
import kotlinx.coroutines.flow.first

/**
 * Creates the example timers once, on first launch (SPEC D9). The flag lives in preferences, so
 * deleting every timer later does not bring the examples back.
 */
class PresetSeeder(
    private val dataStore: DataStore<Preferences>,
    private val timers: TimerRepository,
) {
    /** Returns true when the examples were created by this call. */
    suspend fun seedIfNeeded(names: Presets.Names, timerFallbackName: String): Boolean {
        if (dataStore.data.first()[DID_SEED] == true) return false
        for (preset in Presets.all(names)) {
            timers.add(preset.name, preset.definition, timerFallbackName)
        }
        dataStore.edit { it[DID_SEED] = true }
        return true
    }

    private companion object {
        val DID_SEED = booleanPreferencesKey("didSeedPresets")
    }
}
