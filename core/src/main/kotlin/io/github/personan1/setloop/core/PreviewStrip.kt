package io.github.personan1.setloop.core

/** Colored preview of a timer's structure, with the level of detail picked by a segment threshold (DESIGN V4). */
data class PreviewStrip(val level: Level, val groups: List<Group>) {
    data class Segment(val type: StepType, val duration: Int) {
        constructor(step: Step) : this(step.type, step.duration)
    }

    /** A run of segments drawn together; `repeats > 1` is shown as "xN" above it. */
    data class Group(val segments: List<Segment>, val repeats: Int)

    enum class Level { Workout, OneSet, OneCycle, Blocks }

    companion object {
        /** Not a user setting; tune on a device (DESIGN V4). */
        const val THRESHOLD = 24

        fun from(definition: TimerDefinition, threshold: Int = THRESHOLD): PreviewStrip {
            val sets = maxOf(definition.sets, 1)
            val oneSetCount = definition.blocks.sumOf { it.steps.size * maxOf(it.repeats, 1) }
            val cycleSteps = definition.blocks.sumOf { it.steps.size }

            // The plan and the set are built only when they are short enough to be drawn.
            return when {
                definition.stepCount <= threshold -> PreviewStrip(
                    Level.Workout,
                    listOf(Group(definition.plan.steps.map { Segment(it.type, it.duration) }, repeats = 1)),
                )

                sets > 1 && oneSetCount <= threshold -> {
                    val oneSet = definition.blocks.flatMap { block ->
                        List(maxOf(block.repeats, 1)) { block.steps }.flatten()
                    }
                    PreviewStrip(Level.OneSet, listOf(Group(oneSet.map(::Segment), repeats = sets)))
                }

                cycleSteps <= threshold -> PreviewStrip(
                    Level.OneCycle,
                    definition.blocks.map { Group(it.steps.map(::Segment), maxOf(it.repeats, 1)) },
                )

                else -> PreviewStrip(
                    Level.Blocks,
                    definition.blocks.map { block ->
                        val longest = block.steps.maxByOrNull { it.duration }
                        val total = block.steps.sumOf { it.duration }
                        Group(
                            listOf(Segment(longest?.type ?: StepType.WorkA, total)),
                            repeats = maxOf(block.repeats, 1),
                        )
                    },
                )
            }
        }
    }
}
