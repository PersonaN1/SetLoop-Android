package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.Cue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Side effects of a running workout, implemented by the foreground service. Called on the controller's thread. */
interface WorkoutEffects {
    fun onWorkoutStarted(options: WorkoutOptions)

    fun onSignal(signal: WorkoutSignal)

    fun onStateChanged(state: WorkoutUiState)
}

interface SnapshotStore {
    fun read(): WorkoutSnapshot?

    fun write(snapshot: WorkoutSnapshot)

    fun clear()
}

/**
 * The single running workout of the process (TECH T9): drives the session, sleeps until the next cue
 * instead of ticking, persists a snapshot on every command and publishes [state].
 * All calls must come from one thread, the main thread in the app.
 */
class WorkoutController(
    private val clock: WorkoutClock,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val state: StateFlow<WorkoutUiState> = mutableState.asStateFlow()

    private var session: WorkoutSession? = null
    private var effects: WorkoutEffects? = null
    private var store: SnapshotStore? = null
    private var cueJob: Job? = null

    /** Called by the service when it is created. */
    fun attach(effects: WorkoutEffects, store: SnapshotStore) {
        this.effects = effects
        this.store = store
    }

    /** The service is gone: the workout stops here, but the snapshot stays for a sticky restart. */
    fun detach() {
        cueJob?.cancel()
        cueJob = null
        if (session != null) {
            session = null
            mutableState.value = WorkoutUiState.Idle
        }
        effects = null
        store = null
    }

    /** False when a workout is already running. */
    fun start(request: WorkoutRequest): Boolean {
        if (session != null) return false
        val started = WorkoutSession.start(request, clock.now())
        open(started, started.stepSignal())
        return true
    }

    /** Rebuilds the workout from the stored snapshot after the process was killed. */
    fun restore(): Boolean {
        if (session != null) return true
        val snapshot = store?.read() ?: return false
        val restored = WorkoutSession.restore(snapshot, clock.now())
        if (restored == null) {
            store?.clear()
            return false
        }
        open(restored, emptyList())
        return true
    }

    fun pause() = command(WorkoutCommand.Pause)

    fun resume() = command(WorkoutCommand.Resume)

    fun togglePause() = command(if (mutableState.value.isPaused) WorkoutCommand.Resume else WorkoutCommand.Pause)

    fun skip() = command(WorkoutCommand.Skip)

    fun back() = command(WorkoutCommand.Back)

    fun finish() = command(WorkoutCommand.Finish)

    /** Closes the result screen. */
    fun dismissResult() {
        if (mutableState.value is WorkoutUiState.Finished) mutableState.value = WorkoutUiState.Idle
    }

    private fun open(opened: WorkoutSession, signals: List<WorkoutSignal>) {
        session = opened
        effects?.onWorkoutStarted(opened.request.options)
        publish(signals, persist = true)
    }

    private fun command(command: WorkoutCommand) {
        val current = session ?: return
        val signals = current.apply(command, clock.now())
        publish(signals, persist = true)
    }

    private fun publish(signals: List<WorkoutSignal>, persist: Boolean) {
        val current = session ?: return
        val ui = current.uiState(clock.now())
        // A countdown tick changes nothing the UI or the notification shows.
        val changed = !ui.isSameStepAs(mutableState.value)
        if (changed) mutableState.value = ui
        if (current.isFinished) {
            cueJob?.cancel()
            cueJob = null
            session = null
            store?.clear()
        } else {
            if (persist) store?.write(current.snapshot())
            scheduleCue(current)
        }
        // The notification goes first: audio work must not hold it back.
        if (changed) effects?.onStateChanged(ui)
        signals.forEach { effects?.onSignal(it) }
    }

    /** Sleeps until the next countdown tick or step end. */
    private fun scheduleCue(current: WorkoutSession) {
        cueJob?.cancel()
        val cue = current.nextCue(clock.now()) ?: return
        cueJob = scope.launch {
            // The delay may end a moment early; a cue must never fire before its time.
            while (clock.now() < cue.time) delay(cue.time - clock.now())
            handle(current, cue)
        }
    }

    private fun handle(current: WorkoutSession, cue: Cue) {
        cueJob = null
        if (session !== current) return
        publish(current.fire(cue, clock.now()), persist = false)
    }

    private fun WorkoutUiState.isSameStepAs(other: WorkoutUiState): Boolean =
        this is WorkoutUiState.Active && other is WorkoutUiState.Active &&
            stepIndex == other.stepIndex && stepEnd == other.stepEnd && isPaused == other.isPaused

    private val WorkoutUiState.isPaused: Boolean get() = this is WorkoutUiState.Active && isPaused

    companion object {
        /** Process-wide instance. */
        val instance: WorkoutController by lazy {
            WorkoutController(ElapsedRealtimeClock, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
        }
    }
}
