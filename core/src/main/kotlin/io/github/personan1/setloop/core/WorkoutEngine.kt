package io.github.personan1.setloop.core

import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Time-based workout state machine (TECH T9).
 *
 * The engine never ticks and owns no clock: all state is derived from timestamps passed in by the caller,
 * as `kotlin.time.Duration` offsets on any monotonic clock (the app uses elapsed realtime, tests use plain numbers).
 */
class WorkoutEngine(val plan: WorkoutPlan) {
    sealed interface Phase {
        data object NotStarted : Phase
        data object Running : Phase
        data object Paused : Phase
        data class Finished(val status: WorkoutStatus) : Phase
    }

    var phase: Phase = Phase.NotStarted
        private set
    var currentIndex = 0
        private set

    /** Steps actually reached, in plan order (SPEC D10). */
    val log: List<StepRecord> get() = _log
    private val _log = mutableListOf<StepRecord>()

    private var stepElapsedBase = Duration.ZERO
    private var activeTimeBase = Duration.ZERO
    private var runningSince: Duration? = null

    // Queries

    val currentStep: PlannedStep? get() = plan.steps.getOrNull(currentIndex)

    val nextStep: PlannedStep? get() = plan.steps.getOrNull(currentIndex + 1)

    fun stepElapsed(now: Duration): Duration = stepElapsedBase + (runningSince?.let { now - it } ?: Duration.ZERO)

    fun stepRemaining(now: Duration): Duration {
        val step = currentStep ?: return Duration.ZERO
        return maxOf(step.duration.seconds - stepElapsed(now), Duration.ZERO)
    }

    /** Time spent running, without pauses. Repeated steps (after "Back") count again. */
    fun activeTime(now: Duration): Duration = activeTimeBase + (runningSince?.let { now - it } ?: Duration.ZERO)

    /** The next moment something audible happens; the controller sleeps until then. */
    fun nextCue(now: Duration): Cue? {
        val step = currentStep ?: return null
        val since = runningSince ?: return null
        if (phase != Phase.Running) return null
        val stepEnd = since - stepElapsedBase + step.duration.seconds
        if (step.duration > COUNTDOWN_SECONDS) {
            for (left in COUNTDOWN_SECONDS downTo 1) {
                val time = stepEnd - left.seconds
                if (time > now) return Cue(time, Cue.Kind.Countdown(left))
            }
        }
        return Cue(maxOf(stepEnd, now), Cue.Kind.StepEnd)
    }

    // Commands

    fun start(now: Duration) {
        if (phase != Phase.NotStarted) return
        if (plan.steps.isEmpty()) {
            phase = Phase.Finished(WorkoutStatus.Complete)
            return
        }
        phase = Phase.Running
        runningSince = now
    }

    /** Moves through steps that ended by [now]. Returns what happened, oldest first. */
    fun update(now: Duration): List<Transition> {
        if (phase != Phase.Running) return emptyList()
        val transitions = mutableListOf<Transition>()
        while (true) {
            val step = currentStep ?: break
            if (stepElapsed(now) < step.duration.seconds) break
            val overflow = stepElapsed(now) - step.duration.seconds
            _log.add(StepRecord(currentIndex, step.step, StepRecord.Outcome.Completed, step.duration))
            advance(startingAt = now - overflow)
            transitions.add(if (phase == Phase.Running) Transition.StepStarted(currentIndex) else Transition.Finished)
        }
        return transitions
    }

    fun pause(now: Duration) {
        update(now)
        val since = runningSince
        if (phase != Phase.Running || since == null) return
        stepElapsedBase += now - since
        activeTimeBase += now - since
        runningSince = null
        phase = Phase.Paused
    }

    fun resume(now: Duration) {
        if (phase != Phase.Paused) return
        runningSince = now
        phase = Phase.Running
    }

    fun skip(now: Duration) {
        update(now)
        val step = currentStep
        if (!isActive || step == null) return
        _log.add(StepRecord(currentIndex, step.step, StepRecord.Outcome.Skipped, seconds(stepElapsed(now))))
        advance(startingAt = now)
    }

    /** Restarts the current step, or goes to the previous one within the first 2 s (SPEC D3). */
    fun back(now: Duration) {
        update(now)
        if (!isActive) return
        if (stepElapsed(now) < BACK_THRESHOLD && currentIndex > 0) {
            currentIndex -= 1
            _log.removeAll { it.planIndex >= currentIndex }
        }
        restartStep(now)
    }

    /** Early finish from pause (SPEC D3). The current step is recorded as interrupted. */
    fun finish(now: Duration) {
        update(now)
        if (!isActive) return
        val step = currentStep
        if (step != null && stepElapsed(now) > Duration.ZERO) {
            _log.add(StepRecord(currentIndex, step.step, StepRecord.Outcome.Interrupted, seconds(stepElapsed(now))))
        }
        stopClock(now)
        phase = Phase.Finished(WorkoutStatus.Partial)
    }

    // Private

    private val isActive: Boolean get() = phase == Phase.Running || phase == Phase.Paused

    private fun seconds(time: Duration): Int = time.toDouble(DurationUnit.SECONDS).roundToInt()

    private fun advance(startingAt: Duration) {
        val wasRunning = phase == Phase.Running
        val since = runningSince
        if (wasRunning && since != null) activeTimeBase += startingAt - since
        currentIndex += 1
        stepElapsedBase = Duration.ZERO
        if (currentStep == null) {
            runningSince = null
            phase = Phase.Finished(WorkoutStatus.Complete)
        } else {
            runningSince = if (wasRunning) startingAt else null
        }
    }

    private fun restartStep(now: Duration) {
        runningSince?.let { since ->
            activeTimeBase += now - since
            runningSince = now
        }
        stepElapsedBase = Duration.ZERO
    }

    private fun stopClock(now: Duration) {
        runningSince?.let { activeTimeBase += now - it }
        runningSince = null
    }

    companion object {
        /** Within this long of a step start, "Back" goes to the previous step (SPEC D3). */
        val BACK_THRESHOLD: Duration = 2.seconds

        /** Countdown cues are played this many seconds before a step ends (SPEC D5). */
        const val COUNTDOWN_SECONDS = 3
    }
}

@Serializable
enum class WorkoutStatus {
    /** Reached the end, even with skipped steps. */
    @SerialName("complete") Complete,

    /** Finished early. */
    @SerialName("partial") Partial,
}

@Serializable(with = StepRecordSerializer::class)
data class StepRecord(
    val planIndex: Int,
    val step: Step,
    val outcome: Outcome,
    /** Seconds actually spent on the step: the full duration when completed, less when skipped or interrupted. */
    val elapsed: Int,
) {
    @Serializable
    enum class Outcome {
        @SerialName("completed") Completed,

        /** Only when "Skip" was pressed (SPEC D10). */
        @SerialName("skipped") Skipped,

        /** The step the workout was finished on early. */
        @SerialName("interrupted") Interrupted,
    }
}

@Serializable
private class StepRecordSurrogate(
    val planIndex: Int,
    val step: Step,
    val outcome: StepRecord.Outcome,
    val elapsed: Int? = null,
)

/** Records saved before `elapsed` existed only counted completed steps. */
object StepRecordSerializer : KSerializer<StepRecord> {
    override val descriptor: SerialDescriptor = StepRecordSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: StepRecord) = encoder.encodeSerializableValue(
        StepRecordSurrogate.serializer(),
        StepRecordSurrogate(value.planIndex, value.step, value.outcome, value.elapsed),
    )

    override fun deserialize(decoder: Decoder): StepRecord {
        val surrogate = decoder.decodeSerializableValue(StepRecordSurrogate.serializer())
        val elapsed = surrogate.elapsed
            ?: if (surrogate.outcome == StepRecord.Outcome.Completed) surrogate.step.duration else 0
        return StepRecord(surrogate.planIndex, surrogate.step, surrogate.outcome, elapsed)
    }
}

data class Cue(val time: Duration, val kind: Kind) {
    sealed interface Kind {
        data class Countdown(val secondsLeft: Int) : Kind
        data object StepEnd : Kind
    }
}

sealed interface Transition {
    data class StepStarted(val index: Int) : Transition
    data object Finished : Transition
}
