package io.github.personan1.setloop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class WorkoutEngineTest {
    /** Work 10 / Rest 5 / Work 10. */
    private fun engine() =
        WorkoutEngine(TimerDefinition(blocks = listOf(Block(steps = listOf(step(10), step(5, StepType.Rest), step(10))))).plan)

    @Test
    fun runsToTheEndAndRecordsEveryStep() {
        val e = engine()
        e.start(100.seconds)
        assertTrue(e.update(109.9.seconds).isEmpty())
        assertEquals(listOf(Transition.StepStarted(1)), e.update(110.seconds))
        assertEquals(listOf(Transition.StepStarted(2), Transition.Finished), e.update(125.seconds))
        assertEquals(WorkoutEngine.Phase.Finished(WorkoutStatus.Complete), e.phase)
        assertEquals(List(3) { StepRecord.Outcome.Completed }, e.log.map { it.outcome })
        assertEquals(25.seconds, e.activeTime(200.seconds))
    }

    @Test
    fun catchesUpAfterBeingFrozenInBackground() {
        val e = engine()
        e.start(0.seconds)
        e.update(12.seconds) // 2 s into rest
        assertEquals(1, e.currentIndex)
        assertEquals(3.seconds, e.stepRemaining(12.seconds))
    }

    @Test
    fun pauseFreezesTime() {
        val e = engine()
        e.start(0.seconds)
        e.pause(4.seconds)
        assertEquals(6.seconds, e.stepRemaining(1000.seconds))
        e.resume(1000.seconds)
        assertEquals(3.seconds, e.stepRemaining(1003.seconds))
        e.update(1006.seconds)
        assertEquals(1, e.currentIndex)
        assertEquals(10.seconds, e.activeTime(1006.seconds))
    }

    @Test
    fun skipMarksOnlyThatStep() {
        val e = engine()
        e.start(0.seconds)
        e.skip(3.seconds)
        assertEquals(1, e.currentIndex)
        assertEquals(5.seconds, e.stepRemaining(3.seconds))
        e.update(30.seconds)
        assertEquals(
            listOf(StepRecord.Outcome.Skipped, StepRecord.Outcome.Completed, StepRecord.Outcome.Completed),
            e.log.map { it.outcome },
        )
        assertEquals(WorkoutEngine.Phase.Finished(WorkoutStatus.Complete), e.phase)
    }

    @Test
    fun backRestartsOrGoesToPrevious() {
        val e = engine()
        e.start(0.seconds)
        e.update(13.seconds) // rest, 3 s in
        e.back(13.seconds)
        assertEquals(1, e.currentIndex)
        assertEquals(5.seconds, e.stepRemaining(13.seconds))

        e.back(14.seconds) // 1 s in -> previous
        assertEquals(0, e.currentIndex)
        assertEquals(10.seconds, e.stepRemaining(14.seconds))
        assertTrue(e.log.isEmpty())
    }

    @Test
    fun backOnFirstStepRestartsIt() {
        val e = engine()
        e.start(0.seconds)
        e.back(1.seconds)
        assertEquals(0, e.currentIndex)
        assertEquals(10.seconds, e.stepRemaining(1.seconds))
    }

    @Test
    fun earlyFinishIsPartialAndDropsLaterSteps() {
        val e = engine()
        e.start(0.seconds)
        e.update(12.seconds)
        e.pause(12.seconds)
        e.finish(20.seconds)
        assertEquals(WorkoutEngine.Phase.Finished(WorkoutStatus.Partial), e.phase)
        assertEquals(listOf(StepRecord.Outcome.Completed, StepRecord.Outcome.Interrupted), e.log.map { it.outcome })
        assertEquals(12.seconds, e.activeTime(20.seconds))
    }

    @Test
    fun cuesCountDownThenEndStep() {
        val e = engine()
        e.start(0.seconds)
        assertEquals(Cue(7.seconds, Cue.Kind.Countdown(3)), e.nextCue(0.seconds))
        assertEquals(Cue(8.seconds, Cue.Kind.Countdown(2)), e.nextCue(7.seconds))
        assertEquals(Cue(10.seconds, Cue.Kind.StepEnd), e.nextCue(9.5.seconds))
        e.pause(5.seconds)
        assertNull(e.nextCue(5.seconds))
    }

    @Test
    fun shortStepsHaveNoCountdown() {
        val e = WorkoutEngine(TimerDefinition(blocks = listOf(Block(steps = listOf(step(3))))).plan)
        e.start(0.seconds)
        assertEquals(Cue(3.seconds, Cue.Kind.StepEnd), e.nextCue(0.seconds))
    }
}
