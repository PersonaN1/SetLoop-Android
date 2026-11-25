package io.github.personan1.setloop.data

import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepRecord
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.core.WorkoutSummary
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class HistoryRepositoryTest {
    private lateinit var database: SetLoopDatabase

    // Wednesday; with US rules the week is Sun 2026-11-22 .. Sat 2026-11-28, with ISO rules Mon 23 .. Sun 29.
    private val now = fixedClock("2026-11-25T12:00:00Z")

    @BeforeTest
    fun setUp() {
        database = inMemoryDatabase()
    }

    @AfterTest
    fun tearDown() = database.close()

    private fun history(maxRecords: Int = Limits.maxHistoryRecords, locale: Locale = Locale.US) =
        HistoryRepository(database, { now }, { locale }, maxRecords)

    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    private fun summary(duration: Int = 60, status: WorkoutStatus = WorkoutStatus.Partial) = WorkoutSummary(
        status = status,
        duration = duration,
        steps = listOf(
            StepRecord(0, Step(name = "Prepare", duration = 5, type = StepType.Prepare), StepRecord.Outcome.Completed, 5),
            StepRecord(1, Step(name = "Work", duration = 20, type = StepType.WorkA), StepRecord.Outcome.Completed, 20),
            StepRecord(2, Step(name = "Rest", duration = 10, type = StepType.Rest), StepRecord.Outcome.Skipped, 4),
            StepRecord(3, Step(name = "Work", duration = 20, type = StepType.WorkA), StepRecord.Outcome.Interrupted, 7),
        ),
        totalSteps = 6,
    )

    private suspend fun HistoryRepository.saveAt(iso: String, duration: Int = 60, name: String = "Run") =
        save(name, "", at(iso), summary(duration), sampleDefinition(), "Timer")

    @Test
    fun `save stores all record fields`() = runTest {
        val repository = history()
        val definition = sampleDefinition()
        val workout = summary()

        val saved = repository.save(
            "  Morning  ", " felt good ", at("2026-11-25T08:00:00Z"), workout, definition, "Timer",
        )
        val loaded = assertNotNull(repository.get(saved.id))

        assertEquals("Morning", loaded.name)
        assertEquals("felt good", loaded.note)
        assertEquals(at("2026-11-25T08:00:00Z"), loaded.startedAt)
        assertEquals(60, loaded.duration)
        assertEquals(WorkoutStatus.Partial, loaded.status)
        assertEquals(27, loaded.workTime)
        assertEquals(4, loaded.restTime)
        assertEquals(5, loaded.otherTime)
        assertEquals(2, loaded.completedSteps)
        assertEquals(6, loaded.totalSteps)
        assertEquals(workout.steps, loaded.steps)
        assertEquals(definition, loaded.definition)
    }

    @Test
    fun `blank name falls back to the timer name`() = runTest {
        val saved = history().save(" ", "", 0L, summary(), sampleDefinition(), "Tabata")

        assertEquals("Tabata", saved.name)
    }

    @Test
    fun `saving at the limit removes the oldest records in the same call`() = runTest {
        val repository = history(maxRecords = 3)
        repository.saveAt("2026-11-01T10:00:00Z", name = "oldest")
        repository.saveAt("2026-11-03T10:00:00Z", name = "newest")
        repository.saveAt("2026-11-02T10:00:00Z", name = "middle")

        repository.saveAt("2026-11-04T10:00:00Z", name = "latest")

        assertEquals(listOf("latest", "newest", "middle"), repository.observeRecords().first().map { it.name })
    }

    @Test
    fun `records are not trimmed below the limit`() = runTest {
        val repository = history(maxRecords = 3)
        repeat(3) { repository.saveAt("2026-11-0${it + 1}T10:00:00Z") }

        assertEquals(3, repository.observeRecords().first().size)
    }

    @Test
    fun `delete many removes only the given records`() = runTest {
        val repository = history()
        val first = repository.saveAt("2026-11-01T10:00:00Z", name = "first")
        repository.saveAt("2026-11-02T10:00:00Z", name = "second")
        val third = repository.saveAt("2026-11-03T10:00:00Z", name = "third")

        repository.deleteMany(listOf(first.id, third.id))

        assertEquals(listOf("second"), repository.observeRecords().first().map { it.name })
    }

    @Test
    fun `update text trims the name and note`() = runTest {
        val repository = history()
        val saved = repository.saveAt("2026-11-01T10:00:00Z", name = "old")

        repository.updateText(saved.id, "  new name ", "  a note ")

        val loaded = assertNotNull(repository.get(saved.id))
        assertEquals("new name", loaded.name)
        assertEquals("a note", loaded.note)
    }

    @Test
    fun `update text with a blank name keeps the name and saves the note`() = runTest {
        val repository = history()
        val saved = repository.saveAt("2026-11-01T10:00:00Z", name = "keep")

        repository.updateText(saved.id, "   ", "note")

        val loaded = assertNotNull(repository.get(saved.id))
        assertEquals("keep", loaded.name)
        assertEquals("note", loaded.note)
    }

    @Test
    fun `days group records newest first`() = runTest {
        val repository = history()
        repository.saveAt("2026-11-24T08:00:00Z", name = "a")
        repository.saveAt("2026-11-25T08:00:00Z", name = "b")
        repository.saveAt("2026-11-24T18:00:00Z", name = "c")

        val days = repository.observeDays().first()

        assertEquals(listOf(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 24)), days.map { it.day })
        assertEquals(listOf("c", "a"), days[1].records.map { it.name })
    }

    @Test
    fun `week metrics count the current week only`() = runTest {
        val repository = history()
        repository.saveAt("2026-11-21T23:59:00Z", duration = 1000) // Saturday before
        repository.saveAt("2026-11-22T00:00:00Z", duration = 100) // Sunday, first day
        repository.saveAt("2026-11-25T08:00:00Z", duration = 200)
        repository.saveAt("2026-11-28T23:59:00Z", duration = 300)
        repository.saveAt("2026-11-29T00:00:00Z", duration = 5000) // next week

        assertEquals(RecordTotals(count = 3, totalDuration = 600), repository.observeWeekMetrics().first())
    }

    @Test
    fun `week metrics follow the first day of the week of the locale`() = runTest {
        val repository = history(locale = Locale.GERMANY)
        repository.saveAt("2026-11-22T10:00:00Z", duration = 100) // Sunday of the previous ISO week
        repository.saveAt("2026-11-23T00:00:00Z", duration = 200)
        repository.saveAt("2026-11-29T10:00:00Z", duration = 300)

        assertEquals(RecordTotals(count = 2, totalDuration = 500), repository.observeWeekMetrics().first())
    }

    @Test
    fun `week metrics are zero without records`() = runTest {
        assertEquals(RecordTotals(0, 0), history().observeWeekMetrics().first())
    }
}
