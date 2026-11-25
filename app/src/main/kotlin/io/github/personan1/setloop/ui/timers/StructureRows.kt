package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.TimerDefinition

/** Rows of the read-only structure list on the preview (DESIGN V9). */
sealed interface StructureRow {
    /** A step; [indented] for steps inside a block. */
    data class StepRow(val step: Step, val indented: Boolean) : StructureRow

    /** Shown only when there is more than one set. [rest] is the rest between sets, when set. */
    data class SetsRow(val count: Int, val rest: Step?) : StructureRow

    /** [repeats] is shown only above 1. */
    data class BlockRow(val number: Int, val repeats: Int) : StructureRow
}

fun TimerDefinition.structureRows(): List<StructureRow> = buildList {
    prepare?.let { add(StructureRow.StepRow(it, indented = false)) }
    if (sets > 1) add(StructureRow.SetsRow(sets, restBetweenSets))
    blocks.forEachIndexed { index, block ->
        add(StructureRow.BlockRow(index + 1, block.repeats))
        block.steps.forEach { add(StructureRow.StepRow(it, indented = true)) }
    }
    cooldown?.let { add(StructureRow.StepRow(it, indented = false)) }
}
