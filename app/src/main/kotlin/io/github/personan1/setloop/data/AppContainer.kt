package io.github.personan1.setloop.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.personan1.setloop.tips.TipsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Manual DI: the database, repositories and settings, created once by the `Application`. */
class AppContainer(context: Context, scope: CoroutineScope) {
    private val appContext = context.applicationContext

    val database: SetLoopDatabase = SetLoopDatabase.create(appContext)
    val timers = TimerRepository(database)
    val history = HistoryRepository(database)
    val settings = AppSettings(appContext.settingsDataStore)
    val tips = TipsStore(appContext.settingsDataStore)

    /** First-launch seeding and renaming of default names to the current language (as iOS does at launch). */
    val initialization: Job = scope.launch {
        PresetSeeder(appContext.settingsDataStore, timers)
            .seedIfNeeded(DefaultNames.presetNames(appContext), DefaultNames.newTimerName(appContext))
        timers.relocalize(DefaultNames.from(appContext))
    }
}
