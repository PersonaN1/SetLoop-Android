package io.github.personan1.setloop.tips

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [TipsState] persisted in Preferences DataStore. Tips are local only: no network, no tracking. */
class TipsStore(private val dataStore: DataStore<Preferences>) {
    val state: Flow<TipsState> = dataStore.data.map { it.toState() }

    suspend fun dismiss(tip: Tip) = update { it.dismiss(tip) }

    suspend fun openedTimer() = update { it.openedTimer() }

    suspend fun editorLeft() = update { it.editorLeft() }

    /** Settings > Show Tips Again. */
    suspend fun showAgain() = update { TipsState() }

    private suspend fun update(transform: (TipsState) -> TipsState) {
        dataStore.edit { preferences ->
            val next = transform(preferences.toState())
            preferences[Keys.dismissed] = next.dismissed
            preferences[Keys.introDone] = next.introDone
            preferences[Keys.openedTimer] = next.openedTimer
            preferences[Keys.blockTipDone] = next.blockTipDone
        }
    }

    private fun Preferences.toState() = TipsState(
        dismissed = this[Keys.dismissed].orEmpty(),
        introDone = this[Keys.introDone] ?: false,
        openedTimer = this[Keys.openedTimer] ?: false,
        blockTipDone = this[Keys.blockTipDone] ?: false,
    )

    private object Keys {
        val dismissed = stringSetPreferencesKey("tips.dismissed")
        val introDone = booleanPreferencesKey("tips.introDone")
        val openedTimer = booleanPreferencesKey("tips.openedTimer")
        val blockTipDone = booleanPreferencesKey("tips.blockTipDone")
    }
}
