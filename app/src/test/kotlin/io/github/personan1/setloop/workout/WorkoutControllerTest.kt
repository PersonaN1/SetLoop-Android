package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.WorkoutStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutControllerTest {
    private class RecordingEffects(private val clock: WorkoutClock) : WorkoutEffects {
        val signals = mutableListOf<Pair<Long, WorkoutSignal>>()
        val states = mutableListOf<WorkoutUiState>()
        var options: WorkoutOptions? = null

        override fun onWorkoutStarted(options: WorkoutOptions) {
            this.options = options
        }

        override fun onSignal(signal: WorkoutSignal) {
            signals += clock.now().inWholeMilliseconds to signal
        }

        override fun onStateChanged(state: WorkoutUiState) {
            states += state
        }
    }

    private class MemoryStore : SnapshotStore {
        var snapshot: WorkoutSnapshot? = null
        var writes = 0

        override fun read() = snapshot

        override fun write(snapshot: WorkoutSnapshot) {
            this.snapshot = snapshot
            writes++
        }

        override fun clear() {
            snapshot = null
        }
    }

    /** The virtual time of the test is the workout clock, plus [offset] to simulate a later process. */
    private class Fixture(scope: TestScope, offset: Long = 0, val store: MemoryStore = MemoryStore()) {
        val clock = WorkoutClock { (scope.currentTime + offset).milliseconds }
        val effects = RecordingEffects(clock)
        val controller = WorkoutController(clock, scope.backgroundScope).also { it.attach(effects, store) }
    }

    private fun TestScope.fixture(offset: Long = 0, store: MemoryStore = MemoryStore()) = Fixture(this, offset, store)

    @Test
    fun `plays the start signal, the countdown and the step changes at the right times`() = runTest {
        val fixture = fixture()
        assertTrue(fixture.controller.start(request()))
        advanceTimeBy(15_000)
        runCurrent()

        val times = fixture.effects.signals.map { it.first }
        assertEquals(listOf(0L, 7_000, 8_000, 9_000, 10_000, 12_000, 13_000, 14_000, 15_000), times)
        val signals = fixture.effects.signals.map { it.second }
        assertEquals(StepType.WorkA, assertIs<WorkoutSignal.StepStarted>(signals[0]).step.type)
        assertEquals(listOf(3, 2, 1), signals.subList(1, 4).map { (it as WorkoutSignal.Countdown).secondsLeft })
        assertEquals(StepType.Rest, assertIs<WorkoutSignal.StepStarted>(signals[4]).step.type)
        assertEquals(WorkoutSignal.Finished, signals.last())
    }

    @Test
    fun `publishes state only on step change, not on countdown ticks`() = runTest {
        val fixture = fixture()
        fixture.controller.start(request())
        advanceTimeBy(15_000)
        runCurrent()

        assertEquals(3, fixture.effects.states.size)
        assertEquals(listOf(0, 1), fixture.effects.states.filterIsInstance<WorkoutUiState.Active>().map { it.stepIndex })
        val result = fixture.controller.state.value.finished().result
        assertEquals(WorkoutStatus.Complete, result.summary.status)
        assertEquals(15, result.summary.duration)
    }

    @Test
    fun `passes the options to the effects`() = runTest {
        val fixture = fixture()
        val options = WorkoutOptions(voicePrompts = true, vibration = false)
        fixture.controller.start(request(options = options))
        assertEquals(options, fixture.effects.options)
    }

    @Test
    fun `a second start is rejected while a workout runs`() = runTest {
        val fixture = fixture()
        assertTrue(fixture.controller.start(request()))
        assertFalse(fixture.controller.start(request()))
    }

    @Test
    fun `pause stops the cues and resume schedules them again`() = runTest {
        val fixture = fixture()
        fixture.controller.start(request())
        advanceTimeBy(3_000)
        fixture.controller.pause()
        advanceTimeBy(100_000)
        runCurrent()
        assertEquals(1, fixture.effects.signals.size)
        assertEquals(true, fixture.controller.state.value.active().isPaused)

        fixture.controller.resume()
        advanceTimeBy(7_000)
        runCurrent()
        assertEquals(103_000L + 4_000, fixture.effects.signals[1].first)
        assertEquals(WorkoutSignal.Countdown(3), fixture.effects.signals[1].second)
    }

    @Test
    fun `toggle pause alternates`() = runTest {
        val fixture = fixture()
        fixture.controller.start(request())
        fixture.controller.togglePause()
        assertEquals(true, fixture.controller.state.value.active().isPaused)
        fixture.controller.togglePause()
        assertEquals(false, fixture.controller.state.value.active().isPaused)
    }

    @Test
    fun `skip signals the new step at once and moves the cues`() = runTest {
        val fixture = fixture()
        fixture.controller.start(request())
        advanceTimeBy(2_000)
        fixture.controller.skip()
        assertEquals(2_000L, fixture.effects.signals.last().first)
        assertEquals(StepType.Rest, fixture.controller.state.value.active().step.type)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(WorkoutSignal.Countdown(3), fixture.effects.signals.last().second)
        assertEquals(4_000L, fixture.effects.signals.last().first)
    }

    @Test
    fun `finish ends the workout with a partial result and no signal`() = runTest {
        val fixture = fixture()
        fixture.controller.start(request())
        advanceTimeBy(4_000)
        fixture.controller.finish()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, fixture.effects.signals.size)
        assertEquals(WorkoutStatus.Partial, fixture.controller.state.value.finished().result.summary.status)
    }

    @Test
    fun `dismissing the result goes back to idle and allows a new workout`() = runTest {
        val fixture = fixture()
        fixture.controller.start(request())
        fixture.controller.finish()
        fixture.controller.dismissResult()
        assertEquals(WorkoutUiState.Idle, fixture.controller.state.value)
        assertTrue(fixture.controller.start(request()))
    }

    @Test
    fun `commands without a workout do nothing`() = runTest {
        val fixture = fixture()
        fixture.controller.skip()
        fixture.controller.pause()
        assertEquals(WorkoutUiState.Idle, fixture.controller.state.value)
        assertTrue(fixture.effects.states.isEmpty())
    }

    @Test
    fun `snapshot is written on every command and cleared at the end`() = runTest {
        val fixture = fixture()
        fixture.controller.start(request())
        assertEquals(1, fixture.store.snapshot?.commands?.size)
        fixture.controller.pause()
        fixture.controller.resume()
        assertEquals(
            listOf(WorkoutCommand.Start, WorkoutCommand.Pause, WorkoutCommand.Resume),
            fixture.store.snapshot?.commands?.map { it.command },
        )
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(3, fixture.store.writes)
        fixture.controller.finish()
        assertNull(fixture.store.snapshot)
    }

    @Test
    fun `a new process restores a paused workout from the snapshot`() = runTest {
        val first = fixture()
        first.controller.start(request())
        advanceTimeBy(4_000)
        first.controller.pause()
        val expected = first.controller.state.value.active()

        advanceTimeBy(30_000)
        val second = fixture(store = first.store)
        assertTrue(second.controller.restore())
        val restored = second.controller.state.value.active()
        assertEquals(true, restored.isPaused)
        assertEquals(expected.pausedRemaining, restored.pausedRemaining)
        assertEquals(request().options, second.effects.options)
    }

    @Test
    fun `a restored running workout keeps its cues`() = runTest {
        val first = fixture()
        first.controller.start(request())
        advanceTimeBy(2_000)
        val second = fixture(store = first.store)
        assertTrue(second.controller.restore())
        assertEquals(0, second.effects.signals.size)
        advanceTimeBy(5_001)
        assertEquals(WorkoutSignal.Countdown(3), second.effects.signals.single().second)
    }

    @Test
    fun `restore finds nothing without a snapshot and drops one from another clock`() = runTest {
        assertFalse(fixture().controller.restore())

        val stale = MemoryStore().apply {
            snapshot = WorkoutSession.start(request(), 500.seconds).snapshot()
        }
        val fixture = fixture(store = stale)
        assertFalse(fixture.controller.restore())
        assertNull(stale.snapshot)
        assertNotNull(fixture.controller)
    }
}
