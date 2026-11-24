package io.github.personan1.setloop.data

import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.totalDuration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class TimerRepositoryTest {
    private lateinit var database: SetLoopDatabase
    private lateinit var timers: TimerRepository

    @BeforeTest
    fun setUp() {
        database = inMemoryDatabase()
        timers = TimerRepository(database, TickingClock(), maxTimers = 3)
    }

    @AfterTest
    fun tearDown() = database.close()

    private suspend fun addNamed(name: String) =
        assertIs<TimerAddResult.Added>(timers.add(name, sampleDefinition(), DEFAULT_NAME)).timer

    private suspend fun names() = timers.observeTimers().first().map { it.name }

    @Test
    fun `default limit matches the spec`() {
        assertEquals(100, Limits.maxTimers)
    }

    @Test
    fun `add appends in order and trims and caps the name`() = runTest {
        addNamed("  A  ")
        addNamed("B")
        addNamed("x".repeat(Limits.maxNameLength + 10))

        val list = timers.observeTimers().first()
        assertEquals(listOf(0, 1, 2), list.map { it.sortIndex })
        assertEquals("A", list[0].name)
        assertEquals(Limits.maxNameLength, list[2].name.length)
    }

    @Test
    fun `empty names are numbered like iOS`() = runTest {
        addNamed("")
        addNamed("   ")
        addNamed("")

        assertEquals(listOf(DEFAULT_NAME, "$DEFAULT_NAME (2)", "$DEFAULT_NAME (3)"), names())
    }

    @Test
    fun `adding at the limit is refused without throwing`() = runTest {
        repeat(3) { addNamed("T$it") }

        assertEquals(TimerAddResult.LimitReached, timers.add("One more", sampleDefinition(), DEFAULT_NAME))
        assertEquals(3, timers.observeTimers().first().size)
    }

    @Test
    fun `duplicating at the limit is refused`() = runTest {
        val first = addNamed("A")
        addNamed("B")
        addNamed("C")

        assertEquals(TimerAddResult.LimitReached, timers.duplicate(first.id))
    }

    @Test
    fun `duplicate goes right after the original with a numbered name`() = runTest {
        val a = addNamed("A")
        addNamed("B")

        val copy = assertIs<TimerAddResult.Added>(timers.duplicate(a.id)).timer

        assertEquals(listOf("A", "A (2)", "B"), names())
        assertEquals(listOf(0, 1, 2), timers.observeTimers().first().map { it.sortIndex })
        assertEquals(a.definition, copy.definition)
        assertEquals(a.definitionData, copy.definitionData)
    }

    @Test
    fun `duplicate of a numbered name continues the series`() = runTest {
        val a = addNamed("A")
        val second = assertIs<TimerAddResult.Added>(timers.duplicate(a.id)).timer
        assertIs<TimerAddResult.Added>(timers.duplicate(second.id))

        assertEquals(listOf("A", "A (2)", "A (3)"), names())
    }

    @Test
    fun `duplicate of a missing timer reports not found`() = runTest {
        assertEquals(TimerAddResult.NotFound, timers.duplicate("missing"))
    }

    @Test
    fun `reorder rewrites sort indexes`() = runTest {
        val a = addNamed("A")
        val b = addNamed("B")
        val c = addNamed("C")

        timers.reorder(listOf(c.id, a.id, b.id))

        assertEquals(listOf("C", "A", "B"), names())
        assertEquals(listOf(0, 1, 2), timers.observeTimers().first().map { it.sortIndex })
    }

    @Test
    fun `reorder keeps timers missing from the list after the listed ones`() = runTest {
        val a = addNamed("A")
        val b = addNamed("B")
        addNamed("C")

        timers.reorder(listOf(b.id, a.id))

        assertEquals(listOf("B", "A", "C"), names())
    }

    @Test
    fun `delete many removes only the given timers`() = runTest {
        val a = addNamed("A")
        addNamed("B")
        val c = addNamed("C")

        timers.deleteMany(listOf(a.id, c.id))

        assertEquals(listOf("B"), names())
    }

    @Test
    fun `update changes name definition and caches`() = runTest {
        val a = addNamed("A")
        val edited = sampleDefinition(workSeconds = 45)

        val updated = assertNotNull(timers.update(a.id, "Renamed", edited, DEFAULT_NAME))

        assertEquals("Renamed", updated.name)
        assertEquals(edited.totalDuration.toInt(), updated.totalDuration)
        assertEquals(edited, timers.get(a.id)?.definition)
        assertEquals(a.createdAt, updated.createdAt)
        assertEquals(true, updated.updatedAt > a.updatedAt)
    }

    @Test
    fun `update with an empty name numbers among the other timers only`() = runTest {
        val a = addNamed(DEFAULT_NAME)
        val b = addNamed("B")

        assertEquals(DEFAULT_NAME, timers.update(a.id, "", sampleDefinition(), DEFAULT_NAME)?.name)
        assertEquals("$DEFAULT_NAME (2)", timers.update(b.id, "", sampleDefinition(), DEFAULT_NAME)?.name)
    }

    @Test
    fun `update of a missing timer returns null`() = runTest {
        assertNull(timers.update("missing", "X", sampleDefinition(), DEFAULT_NAME))
    }

    @Test
    fun `definition survives the entity round trip in the iOS wire format`() = runTest {
        val definition = sampleDefinition()
        val saved = assertIs<TimerAddResult.Added>(timers.add("A", definition, DEFAULT_NAME)).timer

        val loaded = assertNotNull(timers.get(saved.id))

        assertEquals(definition, loaded.definition)
        assertEquals(definition.toJson(), loaded.definitionData)
        assertEquals(definition.totalDuration.toInt(), loaded.totalDuration)
    }

    @Test
    fun `a damaged definition reads as an empty timer`() = runTest {
        val broken = addNamed("A").copy(definitionData = "{not json")

        assertEquals(TimerDefinition(blocks = emptyList()), broken.definition)
    }

    private companion object {
        const val DEFAULT_NAME = "New Timer"
    }
}
