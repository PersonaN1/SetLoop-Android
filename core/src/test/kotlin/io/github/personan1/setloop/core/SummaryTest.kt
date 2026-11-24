package io.github.personan1.setloop.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SummaryTest {
    /** Prepare 10 / Work 20 / Rest 10 / Work 20 / Cooldown 30. */
    private fun engine(): WorkoutEngine {
        val definition = TimerDefinition(
            prepare = step(10, StepType.Prepare),
            blocks = listOf(Block(steps = listOf(step(20), step(10, StepType.Rest)), repeats = 2)),
            cooldown = step(30, StepType.Cooldown),
            skipLastRest = true,
        )
        return WorkoutEngine(definition.plan)
    }

    @Test
    fun completeWorkoutCountsEveryCategory() {
        val e = engine()
        e.start(0.seconds)
        e.skip(5.seconds) // prepare skipped
        e.update(1000.seconds)
        val summary = assertNotNull(WorkoutSummary.from(e, 1000.seconds))
        assertEquals(WorkoutStatus.Complete, summary.status)
        assertEquals(5, summary.totalSteps) // the final rest is not in the plan
        assertEquals(4, summary.completedSteps)
        assertEquals(40, summary.time(StepType.Category.Work))
        assertEquals(10, summary.time(StepType.Category.Rest))
        assertEquals(35, summary.time(StepType.Category.Other)) // 5 s of the skipped prepare count
        assertEquals(85, summary.duration)
    }

    @Test
    fun earlyFinishKeepsOnlyReachedSteps() {
        val e = engine()
        e.start(0.seconds)
        e.pause(15.seconds) // 5 s into the first work
        e.finish(15.seconds)
        val summary = assertNotNull(WorkoutSummary.from(e, 15.seconds))
        assertEquals(WorkoutStatus.Partial, summary.status)
        assertEquals(listOf(StepRecord.Outcome.Completed, StepRecord.Outcome.Interrupted), summary.steps.map { it.outcome })
        assertEquals(1, summary.completedSteps)
        assertEquals(5, summary.time(StepType.Category.Work)) // the interrupted part counts
        assertEquals(15, summary.duration)
    }

    @Test
    fun oldRecordsWithoutElapsedDecode() {
        val json = """
        [{"planIndex":0,"step":{"id":"00000000-0000-0000-0000-000000000001","name":"Work","type":"workA","duration":20},"outcome":"completed"},
         {"planIndex":1,"step":{"id":"00000000-0000-0000-0000-000000000002","name":"Rest","type":"rest","duration":10},"outcome":"skipped"}]
        """.trimIndent()
        val steps = TimerDefinition.json.decodeFromString<List<StepRecord>>(json)
        assertEquals(listOf(20, 0), steps.map { it.elapsed })
    }

    @Test
    fun noSummaryWhileRunning() {
        val e = engine()
        e.start(0.seconds)
        assertNull(WorkoutSummary.from(e, 1.seconds))
    }
}

class StripLayoutTest {
    @Test
    fun widthsFollowDurations() {
        val segments = StripLayout.segments(listOf(10, 30), width = 101.0, spacing = 1.0)
        assertEquals(listOf(StripLayout.Segment(0.0, 25.0), StripLayout.Segment(26.0, 75.0)), segments)
    }

    @Test
    fun shortStepsGetMinimumWidth() {
        val segments = StripLayout.segments(listOf(1, 1000), width = 101.0, minWidth = 3.0, spacing = 1.0)
        assertEquals(3.0, segments[0].width)
        assertTrue(abs(segments[1].width - 97) < 0.0001)
        assertEquals(4.0, segments[1].x)
    }

    @Test
    fun tooManyStepsArePackedWithoutGaps() {
        val segments = StripLayout.segments(List(100) { 5 }, width = 200.0, minWidth = 3.0, spacing = 1.0)
        assertEquals(100, segments.size)
        assertEquals(2.0, segments[1].x)
        assertTrue(abs(segments.last().x + segments.last().width - 200) < 0.0001)
    }

    @Test
    fun emptyInput() {
        assertTrue(StripLayout.segments(emptyList(), width = 100.0).isEmpty())
    }
}
