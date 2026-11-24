package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.Cue
import io.github.personan1.setloop.core.StepRecord
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.WorkoutStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class WorkoutSessionTest {
    private fun started(at: Int = 0) = WorkoutSession.start(request(), at.seconds)

    @Test
    fun `next cue is the countdown, then the step end`() {
        val session = started(100)
        assertEquals(Cue(107.seconds, Cue.Kind.Countdown(3)), session.nextCue(100.seconds))
        assertEquals(Cue(109.seconds, Cue.Kind.Countdown(1)), session.nextCue(108.seconds))
        assertEquals(Cue(110.seconds, Cue.Kind.StepEnd), session.nextCue(109.seconds))
    }

    @Test
    fun `no cue while paused`() {
        val session = started()
        session.apply(WorkoutCommand.Pause, 2.seconds)
        assertNull(session.nextCue(3.seconds))
    }

    @Test
    fun `on time countdown cue gives a signal`() {
        val session = started()
        val cue = requireNotNull(session.nextCue(0.seconds))
        assertEquals(listOf(WorkoutSignal.Countdown(3)), session.fire(cue, 7.seconds))
    }

    @Test
    fun `stale cue gives no signal`() {
        val session = started()
        val cue = requireNotNull(session.nextCue(0.seconds))
        assertEquals(emptyList(), session.fire(cue, 8.seconds))
    }

    @Test
    fun `step end gives the next step signal and the end gives finished`() {
        val session = started()
        val first = session.fire(Cue(10.seconds, Cue.Kind.StepEnd), 10.seconds)
        assertEquals("Rest", assertIs<WorkoutSignal.StepStarted>(first.single()).step.step.name)
        val last = session.fire(Cue(15.seconds, Cue.Kind.StepEnd), 15.seconds)
        assertEquals(listOf(WorkoutSignal.Finished), last)
        assertEquals(WorkoutStatus.Complete, session.uiState(15.seconds).finished().result.summary.status)
    }

    @Test
    fun `a late step end skips the stale signal but moves on`() {
        val session = started()
        assertEquals(emptyList(), session.fire(Cue(10.seconds, Cue.Kind.StepEnd), 40.seconds))
        assertEquals(WorkoutStatus.Complete, session.uiState(40.seconds).finished().result.summary.status)
    }

    @Test
    fun `pause time does not count against the step`() {
        val session = started()
        session.apply(WorkoutCommand.Pause, 4.seconds)
        val paused = session.uiState(50.seconds).active()
        assertEquals(true, paused.isPaused)
        assertEquals(6.seconds, paused.remaining(50.seconds))
        session.apply(WorkoutCommand.Resume, 60.seconds)
        val resumed = session.uiState(60.seconds).active()
        assertEquals(66.seconds, resumed.stepEnd)
        assertEquals(6.seconds, resumed.remaining(60.seconds))
        assertEquals(11.seconds, resumed.workoutRemaining)
    }

    @Test
    fun `pause and resume only apply in the matching phase`() {
        val session = started()
        assertEquals(emptyList(), session.apply(WorkoutCommand.Resume, 1.seconds))
        session.apply(WorkoutCommand.Pause, 2.seconds)
        session.apply(WorkoutCommand.Pause, 3.seconds)
        assertEquals(8.seconds, session.uiState(9.seconds).active().remaining(9.seconds))
    }

    @Test
    fun `skip announces the next step and records it as skipped`() {
        val session = started()
        val signals = session.apply(WorkoutCommand.Skip, 3.seconds)
        assertEquals("Rest", assertIs<WorkoutSignal.StepStarted>(signals.single()).step.step.name)
        assertEquals(8.seconds, session.uiState(3.seconds).active().stepEnd - 0.seconds)
        assertEquals(listOf(WorkoutSignal.Finished), session.apply(WorkoutCommand.Skip, 4.seconds))
        val steps = session.uiState(4.seconds).finished().result.summary.steps
        assertEquals(listOf(StepRecord.Outcome.Skipped, StepRecord.Outcome.Skipped), steps.map { it.outcome })
    }

    @Test
    fun `back within two seconds goes to the previous step, later restarts the current one`() {
        val session = started()
        session.fire(Cue(10.seconds, Cue.Kind.StepEnd), 10.seconds)
        val previous = session.apply(WorkoutCommand.Back, 11.seconds)
        assertEquals(StepType.WorkA, assertIs<WorkoutSignal.StepStarted>(previous.single()).step.type)
        assertEquals(0, session.uiState(11.seconds).active().stepIndex)
        session.apply(WorkoutCommand.Back, 15.seconds)
        val restarted = session.uiState(15.seconds).active()
        assertEquals(0, restarted.stepIndex)
        assertEquals(25.seconds, restarted.stepEnd)
    }

    @Test
    fun `finish records the interrupted step and gives no signal`() {
        val session = started()
        assertEquals(emptyList(), session.apply(WorkoutCommand.Finish, 4.seconds))
        val summary = session.uiState(4.seconds).finished().result.summary
        assertEquals(WorkoutStatus.Partial, summary.status)
        assertEquals(StepRecord.Outcome.Interrupted, summary.steps.single().outcome)
        assertEquals(4, summary.duration)
    }

    @Test
    fun `commands after the end are ignored`() {
        val session = started()
        session.apply(WorkoutCommand.Finish, 4.seconds)
        assertEquals(emptyList(), session.apply(WorkoutCommand.Skip, 5.seconds))
    }

    @Test
    fun `snapshot survives json and restores the same state`() {
        val session = started(10)
        session.apply(WorkoutCommand.Pause, 14.seconds)
        session.apply(WorkoutCommand.Resume, 30.seconds)
        session.apply(WorkoutCommand.Skip, 31.seconds)
        val snapshot = WorkoutSnapshot.fromJson(session.snapshot().toJson())
        assertEquals(session.snapshot(), snapshot)

        val restored = requireNotNull(WorkoutSession.restore(snapshot, 33.seconds))
        assertEquals(session.uiState(33.seconds), restored.uiState(33.seconds))
        assertEquals(session.snapshot(), restored.snapshot())
    }

    @Test
    fun `restore keeps the journal of finished steps`() {
        val session = started()
        session.apply(WorkoutCommand.Skip, 3.seconds)
        val restored = requireNotNull(WorkoutSession.restore(session.snapshot(), 4.seconds))
        session.apply(WorkoutCommand.Finish, 5.seconds)
        restored.apply(WorkoutCommand.Finish, 5.seconds)
        assertEquals(
            session.uiState(5.seconds).finished().result.summary,
            restored.uiState(5.seconds).finished().result.summary,
        )
    }

    @Test
    fun `restore after the workout would have ended gives a finished result`() {
        val session = started()
        val restored = requireNotNull(WorkoutSession.restore(session.snapshot(), 100.seconds))
        assertEquals(WorkoutStatus.Complete, restored.uiState(100.seconds).finished().result.summary.status)
        assertEquals(15, restored.uiState(100.seconds).finished().result.summary.duration)
    }

    @Test
    fun `restore rejects snapshots from another clock`() {
        val session = started(50)
        assertNull(WorkoutSession.restore(session.snapshot(), 10.seconds))
        assertNull(WorkoutSession.restore(session.snapshot().copy(commands = emptyList()), 60.seconds))
        assertNull(
            WorkoutSession.restore(
                session.snapshot().copy(commands = session.snapshot().commands.map { it.copy(command = WorkoutCommand.Pause) }),
                60.seconds,
            ),
        )
    }
}
