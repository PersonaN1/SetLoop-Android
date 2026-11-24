package io.github.personan1.setloop.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Presets
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.totalDuration
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class PresetSeederTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: SetLoopDatabase
    private lateinit var timers: TimerRepository
    private lateinit var seeder: PresetSeeder

    @BeforeTest
    fun setUp() {
        database = inMemoryDatabase()
        timers = TimerRepository(database, TickingClock())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "seed.preferences_pb") }
        seeder = PresetSeeder(dataStore, timers)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        database.close()
    }

    private suspend fun seed() = seeder.seedIfNeeded(DefaultNames.presetNames(context), DefaultNames.newTimerName(context))

    @Test
    fun `seeds the example timers in the iOS order`() = runTest {
        assertTrue(seed())

        val list = timers.observeTimers().first()
        assertEquals(listOf("Tabata 20/10", "Simple interval", "Your example"), list.map { it.name })
        assertEquals(listOf(0, 1, 2), list.map { it.sortIndex })
        assertEquals(listOf(250, 600, 292), list.map { it.totalDuration })
    }

    @Test
    fun `seeded definitions equal the core presets`() = runTest {
        seed()

        val expected = Presets.all(DefaultNames.presetNames(context))
        val stored = timers.observeTimers().first()
        assertEquals(expected.map { it.name }, stored.map { it.name })
        assertEquals(expected.map { it.definition.withoutIds() }, stored.map { it.definition.withoutIds() })
    }

    @Test
    fun `seeding happens once even after every timer is deleted`() = runTest {
        assertTrue(seed())
        assertFalse(seed())
        assertEquals(3, timers.observeTimers().first().size)

        timers.deleteMany(timers.observeTimers().first().map { it.id })

        assertFalse(seed())
        assertEquals(0, timers.observeTimers().first().size)
    }

    @Test
    @Config(sdk = [TEST_SDK], qualifiers = "ru")
    fun `seeds localized names in Russian`() = runTest {
        seed()

        val list = timers.observeTimers().first()
        assertEquals(listOf("Табата 20/10", "Простой интервал", "Ваш пример"), list.map { it.name })
        assertEquals("Подготовка", list[0].definition.prepare?.name)
        assertEquals("Работа", list[0].definition.blocks.single().steps.first().name)
        assertEquals("Отдых между сетами", list[2].definition.restBetweenSets?.name)
    }

    @Test
    @Config(sdk = [TEST_SDK], qualifiers = "ru")
    fun `relocalize turns stored English default names into Russian and keeps custom names`() = runTest {
        val english = Presets.all(
            Presets.Names("Prepare", "Work", "Rest", "Rest between sets", "Tabata 20/10", "Simple interval", "Your example"),
        )
        english.forEach { timers.add(it.name, it.definition, "New Timer") }
        timers.add("My own", custom(), "New Timer")

        timers.relocalize(DefaultNames.from(context))

        val list = timers.observeTimers().first()
        assertEquals(listOf("Табата 20/10", "Простой интервал", "Ваш пример", "My own"), list.map { it.name })
        assertEquals("Подготовка", list[0].definition.prepare?.name)
        val custom = list[3].definition.blocks.single().steps
        assertEquals(listOf("Работа", "Sprint"), custom.map { it.name })
    }

    @Test
    fun `localize returns the same instance when nothing changes`() {
        val names = DefaultNames.from(context)
        val definition = sampleDefinition()

        assertTrue(names.localize(definition) === definition)
        assertEquals(definition.totalDuration, names.localize(definition).totalDuration)
    }

    private fun TimerDefinition.withoutIds(): TimerDefinition {
        val fixed = UUID(0, 0)
        fun Step.clean() = copy(id = fixed)
        return copy(
            prepare = prepare?.clean(),
            blocks = blocks.map { block -> block.copy(id = fixed, steps = block.steps.map { it.clean() }) },
            restBetweenSets = restBetweenSets?.clean(),
            cooldown = cooldown?.clean(),
        )
    }

    private fun custom() = TimerDefinition(
        blocks = listOf(
            Block(
                steps = listOf(
                    Step(name = "Work", duration = 10, type = StepType.WorkA),
                    Step(name = "Sprint", duration = 10, type = StepType.WorkB),
                ),
            ),
        ),
    )
}
