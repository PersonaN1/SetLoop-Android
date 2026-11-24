package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.Cue
import io.github.personan1.setloop.core.PlannedStep
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.Transition
import io.github.personan1.setloop.core.WorkoutEngine
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.core.WorkoutSummary
import io.github.personan1.setloop.core.plan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class WorkoutRequest(
    val definition: TimerDefinition,
    val timerName: String,
    val options: WorkoutOptions,
    val startedAtEpochMillis: Long,
)

@Serializable
enum class WorkoutCommand { Start, Pause, Resume, Skip, Back, Finish }

@Serializable
data class TimedCommand(val atMillis: Long, val command: WorkoutCommand)

/**
 * Everything needed to rebuild a workout after process death: the engine is deterministic,
 * so replaying the commands reproduces its state and journal (TECH T9).
 */
@Serializable
data class WorkoutSnapshot(
    val definition: TimerDefinition,
    val timerName: String,
    val options: WorkoutOptions,
    val startedAtEpochMillis: Long,
    val commands: List<TimedCommand>,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): WorkoutSnapshot = json.decodeFromString(serializer(), text)
    }
}

/** What the audio, voice and vibration layer should do. */
sealed interface WorkoutSignal {
    data class Countdown(val secondsLeft: Int) : WorkoutSignal
    data class StepStarted(val step: PlannedStep) : WorkoutSignal
    data object Finished : WorkoutSignal
}

/**
 * The engine plus the command log, with no Android and no clock: callers pass the time in.
 * Decides which signals a command or a cue produces.
 */
class WorkoutSession private constructor(val request: WorkoutRequest) {
    private val engine = WorkoutEngine(request.definition.plan)
    private val commandLog = mutableListOf<TimedCommand>()

    val isFinished: Boolean get() = engine.phase is WorkoutEngine.Phase.Finished

    val isRunning: Boolean get() = engine.phase == WorkoutEngine.Phase.Running

    fun nextCue(now: Duration): Cue? = engine.nextCue(now)

    /** Applies a user command and returns the signals it causes: a new step after Skip or Back, the end after the last Skip. */
    fun apply(command: WorkoutCommand, now: Duration): List<WorkoutSignal> {
        if (!accepts(command)) return emptyList()
        commandLog += TimedCommand(now.inWholeMilliseconds, command)
        run(command, now)
        return when (command) {
            WorkoutCommand.Skip, WorkoutCommand.Back -> stepSignal()
            else -> emptyList()
        }
    }

    /** Handles a cue the controller slept until. A late wake-up plays nothing: stale signals are worse than none. */
    fun fire(cue: Cue, now: Duration): List<WorkoutSignal> {
        val onTime = now - cue.time < STALE_AFTER
        val signals = mutableListOf<WorkoutSignal>()
        val kind = cue.kind
        if (kind is Cue.Kind.Countdown && onTime) signals += WorkoutSignal.Countdown(kind.secondsLeft)
        val last = engine.update(now).lastOrNull()
        if (onTime && last != null) signals += signalFor(last)
        return signals
    }

    /** Moves the engine to [now] without any signals, after the process was away. */
    fun catchUp(now: Duration) {
        engine.update(now)
    }

    fun snapshot(): WorkoutSnapshot = WorkoutSnapshot(
        request.definition,
        request.timerName,
        request.options,
        request.startedAtEpochMillis,
        commandLog.toList(),
    )

    fun uiState(now: Duration): WorkoutUiState {
        val step = engine.currentStep
        return when {
            engine.phase is WorkoutEngine.Phase.Finished -> WorkoutUiState.Finished(result(now))
            step == null -> WorkoutUiState.Idle
            else -> {
                val remaining = engine.stepRemaining(now)
                val later = engine.plan.steps.drop(engine.currentIndex + 1).sumOf { it.duration }
                WorkoutUiState.Active(
                    timerName = request.timerName,
                    step = step,
                    next = engine.nextStep,
                    color = request.options.stepColors[step.type],
                    isPaused = engine.phase == WorkoutEngine.Phase.Paused,
                    stepIndex = engine.currentIndex,
                    stepCount = engine.plan.steps.size,
                    stepEnd = now + remaining,
                    pausedRemaining = remaining,
                    workoutRemaining = remaining + later.seconds,
                )
            }
        }
    }

    private fun result(now: Duration) = WorkoutResult(
        timerName = request.timerName,
        definition = request.definition,
        startedAtEpochMillis = request.startedAtEpochMillis,
        summary = requireNotNull(WorkoutSummary.from(engine, now)) { "Workout is not finished" },
    )

    private fun accepts(command: WorkoutCommand): Boolean = when (command) {
        WorkoutCommand.Start -> false
        WorkoutCommand.Pause -> engine.phase == WorkoutEngine.Phase.Running
        WorkoutCommand.Resume -> engine.phase == WorkoutEngine.Phase.Paused
        WorkoutCommand.Skip, WorkoutCommand.Back, WorkoutCommand.Finish ->
            engine.phase == WorkoutEngine.Phase.Running || engine.phase == WorkoutEngine.Phase.Paused
    }

    private fun run(command: WorkoutCommand, now: Duration) = when (command) {
        WorkoutCommand.Start -> engine.start(now)
        WorkoutCommand.Pause -> engine.pause(now)
        WorkoutCommand.Resume -> engine.resume(now)
        WorkoutCommand.Skip -> engine.skip(now)
        WorkoutCommand.Back -> engine.back(now)
        WorkoutCommand.Finish -> engine.finish(now)
    }

    /** The signal for the step the workout is on now, or the end. */
    fun stepSignal(): List<WorkoutSignal> {
        if (engine.phase == WorkoutEngine.Phase.Finished(WorkoutStatus.Complete)) return listOf(WorkoutSignal.Finished)
        return listOfNotNull(engine.currentStep?.takeIf { !isFinished }?.let(WorkoutSignal::StepStarted))
    }

    private fun signalFor(transition: Transition): WorkoutSignal = when (transition) {
        is Transition.StepStarted -> WorkoutSignal.StepStarted(requireNotNull(engine.currentStep))
        Transition.Finished -> WorkoutSignal.Finished
    }

    companion object {
        private val STALE_AFTER = 1.seconds

        fun start(request: WorkoutRequest, now: Duration): WorkoutSession {
            val session = WorkoutSession(request)
            session.commandLog += TimedCommand(now.inWholeMilliseconds, WorkoutCommand.Start)
            session.engine.start(now)
            return session
        }

        /**
         * Null when the snapshot cannot belong to this clock: empty, not starting with Start,
         * or from the future (the device rebooted and the elapsed-realtime clock restarted).
         */
        fun restore(snapshot: WorkoutSnapshot, now: Duration): WorkoutSession? {
            val commands = snapshot.commands
            if (commands.firstOrNull()?.command != WorkoutCommand.Start) return null
            if (commands.zipWithNext().any { (a, b) -> b.atMillis < a.atMillis }) return null
            if (commands.last().atMillis.milliseconds > now) return null
            val session = WorkoutSession(
                WorkoutRequest(snapshot.definition, snapshot.timerName, snapshot.options, snapshot.startedAtEpochMillis),
            )
            session.engine.start(commands.first().atMillis.milliseconds)
            session.commandLog += commands.first()
            for (timed in commands.drop(1)) {
                session.apply(timed.command, timed.atMillis.milliseconds)
            }
            session.catchUp(now)
            return session
        }
    }
}
