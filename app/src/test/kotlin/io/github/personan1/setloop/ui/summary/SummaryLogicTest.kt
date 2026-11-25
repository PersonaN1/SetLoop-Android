package io.github.personan1.setloop.ui.summary

import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepRecord
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.core.WorkoutSummary
import io.github.personan1.setloop.data.HistoryRepository
import io.github.personan1.setloop.data.SetLoopDatabase
import io.github.personan1.setloop.data.TEST_SDK
import io.github.personan1.setloop.data.inMemoryDatabase
import io.github.personan1.setloop.data.sampleDefinition
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class SummaryLogicTest {
    private lateinit var database: SetLoopDatabase
    private lateinit var history: HistoryRepository
    private var dismissed = 0

    private val summary = WorkoutSummary(
        status = WorkoutStatus.Complete,
        duration = 35,
        steps = listOf(StepRecord(0, Step(name = "Work", duration = 20, type = StepType.WorkA), StepRecord.Outcome.Completed, 20)),
        totalSteps = 1,
    )

    @BeforeTest
    fun setUp() {
        database = inMemoryDatabase()
        history = HistoryRepository(database)
    }

    @AfterTest
    fun tearDown() = database.close()

    private fun flow(timerName: String = "Tabata") = SummarySaveFlow(
        persist = { name, note ->
            history.save(name, note, startedAt = 1_000L, summary = summary, definition = sampleDefinition(), defaultName = timerName)
        },
        dismiss = { dismissed++ },
    )

    @Test
    fun `save stores the typed name and note, then dismisses`() = runTest {
        val flow = flow()

        flow.save(SummaryFields("Evening run", "legs"))

        val record = history.observeRecords().first().single()
        assertEquals("Evening run", record.name)
        assertEquals("legs", record.note)
        assertEquals(1, dismissed)
        assertTrue(flow.isSaved.value)
    }

    @Test
    fun `a blank name falls back to the timer name`() = runTest {
        flow(timerName = "Tabata").save(SummaryFields("   ", ""))

        assertEquals("Tabata", history.observeRecords().first().single().name)
    }

    @Test
    fun `a second save is ignored`() = runTest {
        val flow = flow()

        flow.save(SummaryFields("One"))
        flow.save(SummaryFields("Two"))

        assertEquals(listOf("One"), history.observeRecords().first().map { it.name })
        assertEquals(1, dismissed)
    }

    @Test
    fun `discard dismisses without saving`() = runTest {
        val flow = flow()

        flow.discard()

        assertEquals(emptyList(), history.observeRecords().first())
        assertEquals(1, dismissed)
    }

    @Test
    fun `discard after save does not dismiss twice`() = runTest {
        val flow = flow()

        flow.save(SummaryFields("One"))
        flow.discard()

        assertEquals(1, dismissed)
    }

    @Test
    fun `fields are edited when the name changed or a note was typed`() {
        assertFalse(SummaryFields("Tabata").isEdited("Tabata"))
        assertTrue(SummaryFields("Tabata 2").isEdited("Tabata"))
        assertTrue(SummaryFields("Tabata", "hard").isEdited("Tabata"))
        assertFalse(SummaryFields("Tabata", "  ").isEdited("Tabata"))
    }

    @Test
    fun `typed names are capped`() {
        val capped = SummaryFields("").withName("x".repeat(Limits.maxNameLength + 20))

        assertEquals(Limits.maxNameLength, capped.name.length)
    }
}
