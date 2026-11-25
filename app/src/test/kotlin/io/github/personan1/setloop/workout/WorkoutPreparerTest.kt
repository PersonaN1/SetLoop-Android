package io.github.personan1.setloop.workout

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.data.AppSettings
import io.github.personan1.setloop.data.AppSettingsValues
import io.github.personan1.setloop.data.DefaultNames
import io.github.personan1.setloop.data.SetLoopDatabase
import io.github.personan1.setloop.data.TEST_SDK
import io.github.personan1.setloop.data.TickingClock
import io.github.personan1.setloop.data.TimerAddResult
import io.github.personan1.setloop.data.TimerRepository
import io.github.personan1.setloop.data.inMemoryDatabase
import io.github.personan1.setloop.data.sampleDefinition
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], qualifiers = "ru")
class WorkoutPreparerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: SetLoopDatabase
    private lateinit var timers: TimerRepository
    private lateinit var settings: AppSettings
    private lateinit var preparer: WorkoutPreparer

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = inMemoryDatabase()
        timers = TimerRepository(database, TickingClock())
        settings = AppSettings(PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "s.preferences_pb") })
        preparer = WorkoutPreparer(timers, settings) { DefaultNames.from(context) }
    }

    @AfterTest
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    @Test
    fun `loads the timer, localizes default names and reads settings`() = runTest {
        val added = assertIs<TimerAddResult.Added>(timers.add("Tabata 20/10", sampleDefinition(), "New")).timer
        settings.setVibration(false)

        val prepared = assertNotNull(preparer.prepare(added.id))

        assertEquals("Табата 20/10", prepared.timerName)
        assertEquals("Подготовка", prepared.definition.prepare?.name)
        assertEquals("Работа", prepared.definition.blocks.single().steps.first().name)
        assertFalse(prepared.options.vibration)
    }

    @Test
    fun `a stored definition keeps its names and reads settings`() = runTest {
        settings.setVoicePrompts(true)
        val definition = sampleDefinition()

        val prepared = preparer.prepare("Evening", definition)

        assertEquals("Evening", prepared.timerName)
        assertEquals(definition, prepared.definition)
        assertEquals(true, prepared.options.voicePrompts)
    }

    @Test
    fun `unknown timer gives null`() = runTest {
        assertNull(preparer.prepare("missing"))
    }

    @Test
    fun `settings map to workout options`() {
        val values = AppSettingsValues(
            soundSet = SoundSet.Marimba,
            voicePrompts = true,
            vibration = false,
            ignoreSilentMode = false,
            showOnLockScreen = false,
            pauseOnInterruption = false,
        )

        val options = values.toWorkoutOptions()

        assertEquals(SoundSet.Marimba, options.soundSet)
        assertTrue(options.soundsEnabled)
        assertTrue(options.voicePrompts)
        assertFalse(options.vibration)
        assertFalse(options.ignoreSilentMode)
        assertFalse(options.showOnLockScreen)
        assertFalse(options.pauseOnInterruption)
        assertEquals(values.stepColors[StepType.WorkA], options.stepColors[StepType.WorkA])
    }

    @Test
    fun `sound set off disables sounds but keeps a valid set`() {
        val options = AppSettingsValues(soundSet = null).toWorkoutOptions()

        assertFalse(options.soundsEnabled)
        assertEquals(WorkoutOptions().soundSet, options.soundSet)
    }
}
