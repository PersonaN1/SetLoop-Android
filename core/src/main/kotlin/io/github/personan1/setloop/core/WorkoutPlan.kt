package io.github.personan1.setloop.core

/** One step of the flattened plan, with its position in the structure. */
data class PlannedStep(val index: Int, val step: Step, val position: Position) {
    data class Position(
        /** 1-based; null for prepare / cooldown / rest between sets. */
        val set: Int?,
        val setCount: Int,
        val block: Int?,
        val blockCount: Int,
        val cycle: Int? = null,
        val cycleCount: Int? = null,
    )

    val duration: Int get() = step.duration
    val type: StepType get() = step.type
}

/** Linear sequence of steps the engine runs through. */
data class WorkoutPlan(val steps: List<PlannedStep>) {
    /** Seconds. Long because the limits allow totals beyond Int range. */
    val totalDuration: Long get() = steps.sumOf { it.duration.toLong() }

    fun duration(category: StepType.Category): Long =
        steps.filter { it.type.category == category }.sumOf { it.duration.toLong() }

    companion object {
        fun from(definition: TimerDefinition): WorkoutPlan {
            val result = mutableListOf<PlannedStep>()
            val setCount = maxOf(definition.sets, 1)
            val blockCount = definition.blocks.size

            fun append(step: Step, position: PlannedStep.Position) {
                result.add(PlannedStep(result.size, step, position))
            }

            val outside = PlannedStep.Position(set = null, setCount = setCount, block = null, blockCount = blockCount)

            definition.prepare?.let { append(it, outside) }
            for (set in 1..setCount) {
                definition.blocks.forEachIndexed { blockIndex, block ->
                    val cycles = maxOf(block.repeats, 1)
                    for (cycle in 1..cycles) {
                        for (step in block.steps) {
                            append(
                                step,
                                PlannedStep.Position(
                                    set = set, setCount = setCount,
                                    block = blockIndex + 1, blockCount = blockCount,
                                    cycle = cycle, cycleCount = cycles,
                                ),
                            )
                        }
                    }
                }
                if (set < setCount) definition.restBetweenSets?.let { append(it, outside) }
            }
            // The final rest is not part of the plan at all: neither skipped nor counted (SPEC D10, D18).
            val last = result.lastOrNull()
            if (definition.skipLastRest && last != null && last.type == StepType.Rest && last.position.set != null) {
                result.removeAt(result.lastIndex)
            }
            definition.cooldown?.let { append(it, outside) }

            return WorkoutPlan(result)
        }
    }
}

val TimerDefinition.plan: WorkoutPlan get() = WorkoutPlan.from(this)

// Plan arithmetic: totals computed from the structure, because limits allow millions of steps,
// too many to build for a list row.

/** The plan's final set step, when `skipLastRest` drops it. */
private val TimerDefinition.droppedLastRest: Step?
    get() {
        if (!skipLastRest) return null
        val last = blocks.lastOrNull { it.steps.isNotEmpty() }?.steps?.last() ?: return null
        return last.takeIf { it.type == StepType.Rest }
    }

/** Sums `value` over the plan's steps without building it. */
private fun TimerDefinition.planSum(value: (Step) -> Long): Long {
    val setCount = maxOf(sets, 1)
    val perSet = blocks.sumOf { block -> block.steps.sumOf(value) * maxOf(block.repeats, 1) }
    var total = perSet * setCount
    restBetweenSets?.let { total += value(it) * (setCount - 1) }
    droppedLastRest?.let { total -= value(it) }
    prepare?.let { total += value(it) }
    cooldown?.let { total += value(it) }
    return total
}

/** `plan.steps.size`. */
val TimerDefinition.stepCount: Int get() = planSum { 1L }.toInt()

/** `plan.totalDuration`, seconds. */
val TimerDefinition.totalDuration: Long get() = planSum { it.duration.toLong() }

/** `plan.duration(category)`, seconds. */
fun TimerDefinition.duration(category: StepType.Category): Long =
    planSum { if (it.type.category == category) it.duration.toLong() else 0L }
