package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Step
import java.util.UUID

/** One row of the editor list. [key] is stable across edits, so drag and drop and animations follow the row. */
sealed interface EditorRow {
    val key: Any

    /** A step slot outside the blocks; [step] is null while it is not added yet. */
    data class Optional(val slot: StepSlot, val step: Step?) : EditorRow {
        // Lazy list keys must fit into a Bundle, so no slot objects.
        override val key: Any = "optional-${slot::class.simpleName}"
    }

    data object Sets : EditorRow {
        override val key: Any = "sets"
    }

    data class Cycles(val block: Block) : EditorRow {
        override val key: Any = "cycles-${block.id}"
    }

    data class BlockStep(val blockId: UUID, val step: Step) : EditorRow {
        override val key: Any = step.id
    }

    data class AddStep(val blockId: UUID) : EditorRow {
        override val key: Any = "add-step-$blockId"
    }

    data object AddBlock : EditorRow {
        override val key: Any = "add-block"
    }

    data object SkipLastRest : EditorRow {
        override val key: Any = "skip-last-rest"
    }
}

/** The "General" section: prepare, sets, and rest between sets when there is more than one set. */
fun EditorState.generalRows(): List<EditorRow> = buildList {
    add(EditorRow.Optional(StepSlot.Prepare, definition.prepare))
    add(EditorRow.Sets)
    if (definition.sets > 1) add(EditorRow.Optional(StepSlot.RestBetweenSets, definition.restBetweenSets))
}

/** Cycles, the steps and "Add Step" of a block. */
fun blockRows(block: Block): List<EditorRow> = buildList {
    add(EditorRow.Cycles(block))
    block.steps.forEach { add(EditorRow.BlockStep(block.id, it)) }
    add(EditorRow.AddStep(block.id))
}

/** The "Finish" section: cooldown and "Skip Last Rest". */
fun EditorState.finishRows(): List<EditorRow> = listOf(
    EditorRow.Optional(StepSlot.Cooldown, definition.cooldown),
    EditorRow.SkipLastRest,
)
