package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.cappedName

/** Step sheet logic (DESIGN V11, SPEC D18): the step as edited, committed only by Done. */
data class StepDraft(val step: Step) {
    val minutes: Int get() = step.duration / 60
    val seconds: Int get() = step.duration % 60

    /** The wheels pass through 0:00, which no step can have: Done stays disabled there. */
    val canSave: Boolean get() = step.duration in Limits.stepDuration

    fun withName(value: String) = StepDraft(step.copy(name = value.cappedName))

    /** A default name follows the type; a name the user typed stays. */
    fun withType(type: StepType, names: StepNames): StepDraft {
        val trimmed = step.name.trim()
        val followsType = trimmed.isEmpty() || trimmed == names.forType(step.type)
        return StepDraft(step.copy(type = type, name = if (followsType) names.forType(type) else step.name))
    }

    fun withMinutes(value: Int) = withDuration(value * 60 + seconds)

    fun withSeconds(value: Int) = withDuration(minutes * 60 + value)

    /** 0:00 is kept so turning a wheel back returns to the same time. */
    private fun withDuration(total: Int) =
        StepDraft(step.copy(duration = total.coerceIn(0, Limits.stepDuration.last)))

    /** The step to save: an empty name becomes the default name of its type. */
    fun finished(names: StepNames): Step {
        val trimmed = step.name.trim()
        return step.copy(name = trimmed.ifEmpty { names.forType(step.type) })
    }

    companion object {
        const val MAX_MINUTES = 99
        const val MAX_SECONDS = 59
    }
}
