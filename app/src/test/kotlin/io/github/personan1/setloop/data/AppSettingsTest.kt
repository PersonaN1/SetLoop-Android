package io.github.personan1.setloop.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.personan1.setloop.core.RGB
import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.core.StepColors
import io.github.personan1.setloop.core.StepType
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class AppSettingsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "settings.preferences_pb") }
    }
    private val settings by lazy { AppSettings(dataStore) }

    @AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun `defaults match iOS`() = runTest {
        val values = settings.values.first()

        assertEquals(true, values.pauseOnInterruption)
        assertEquals(true, values.showOnLockScreen)
        assertEquals(true, values.keepScreenOn)
        assertEquals(true, values.vibration)
        assertEquals(false, values.voicePrompts)
        assertEquals(true, values.ignoreSilentMode)
        assertEquals(SoundSet.Glass, values.soundSet)
        assertEquals(AppTheme.System, values.theme)
        assertEquals(StepColors.DEFAULT, values.stepColors)
        assertEquals(AppSettingsValues(), values)
    }

    @Test
    fun `every setter is reflected in the flow`() = runTest {
        settings.setPauseOnInterruption(false)
        settings.setShowOnLockScreen(false)
        settings.setKeepScreenOn(false)
        settings.setVibration(false)
        settings.setVoicePrompts(true)
        settings.setIgnoreSilentMode(false)
        settings.setSoundSet(SoundSet.Marimba)
        settings.setTheme(AppTheme.Dark)

        assertEquals(
            AppSettingsValues(
                pauseOnInterruption = false,
                showOnLockScreen = false,
                keepScreenOn = false,
                vibration = false,
                voicePrompts = true,
                ignoreSilentMode = false,
                soundSet = SoundSet.Marimba,
                theme = AppTheme.Dark,
            ),
            settings.values.first(),
        )
    }

    @Test
    fun `sound set off is kept apart from the default`() = runTest {
        settings.setSoundSet(null)
        assertEquals(null, settings.values.first().soundSet)

        settings.setSoundSet(SoundSet.Note)
        assertEquals(SoundSet.Note, settings.values.first().soundSet)
    }

    @Test
    fun `an unknown sound set or theme falls back to the default`() = runTest {
        dataStore.edit {
            it[stringPreferencesKey("soundSet")] = "vanished"
            it[stringPreferencesKey("theme")] = "sepia"
        }

        val values = settings.values.first()

        assertEquals(SoundSet.Glass, values.soundSet)
        assertEquals(AppTheme.System, values.theme)
    }

    @Test
    fun `step colors round trip as JSON`() = runTest {
        val custom = StepColors(StepColors.DEFAULT.colors + (StepType.WorkA to RGB(0x2F6BDB)))

        settings.setStepColors(custom)

        assertEquals(custom, settings.values.first().stepColors)
        assertEquals(RGB(0x2F6BDB), settings.values.first().stepColors[StepType.WorkA])
    }

    @Test
    fun `damaged step colors fall back to the default`() = runTest {
        dataStore.edit { it[stringPreferencesKey("stepColors")] = "{broken" }

        assertEquals(StepColors.DEFAULT, settings.values.first().stepColors)
    }

    @Test
    fun `settings persist in a new store over the same file`() = runTest {
        settings.setTheme(AppTheme.Light)
        scope.cancel()

        val reopenScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val reopened = AppSettings(
            PreferenceDataStoreFactory.create(scope = reopenScope) { File(folder.root, "settings.preferences_pb") },
        )

        assertEquals(AppTheme.Light, reopened.values.first().theme)
        reopenScope.cancel()
    }
}
