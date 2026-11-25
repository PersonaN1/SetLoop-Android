package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.cappedName
import io.github.personan1.setloop.core.mergeIntoBlock
import io.github.personan1.setloop.core.next
import io.github.personan1.setloop.core.removeEmptyBlocks
import java.util.UUID

/** Default step names in the current language; the core keeps them as plain strings. */
data class StepNames(
    val prepare: String,
    val work: String,
    val rest: String,
    val restBetweenSets: String,
    val cooldown: String,
) {
    fun forType(type: StepType): String = when (type) {
        StepType.Prepare -> prepare
        StepType.WorkA, StepType.WorkB, StepType.WorkC -> work
        StepType.Rest -> rest
        StepType.Cooldown -> cooldown
    }
}

/** Where an edited step lives in the definition. */
sealed interface StepSlot {
    data object Prepare : StepSlot
    data object RestBetweenSets : StepSlot
    data object Cooldown : StepSlot

    /** A null [stepId] appends a new step to the block. */
    data class InBlock(val blockId: UUID, val stepId: UUID?) : StepSlot

    /** Prepare, cooldown and rest between sets have a fixed type. */
    val hasFixedType: Boolean get() = this !is InBlock
}

/** A step open in the step sheet. [isNew] steps are not in the definition yet: the sheet offers Cancel, not Delete. */
data class StepEdit(val slot: StepSlot, val step: Step, val isNew: Boolean = false)

sealed interface MergeResult {
    data class Merged(val state: EditorState) : MergeResult
    data class Failed(val error: TimerDefinition.MergeError) : MergeResult
}

/**
 * Editor working copy (SPEC D1, D2, D18): changes touch only this copy until Done. Immutable, so every
 * operation returns a new state; operations refused by a limit return the same instance.
 */
data class EditorState(
    val name: String,
    val definition: TimerDefinition,
    val originalName: String,
    val originalDefinition: TimerDefinition,
    val selecting: Boolean = false,
    val selection: Set<UUID> = emptySet(),
) {
    val hasChanges: Boolean get() = name != originalName || definition != originalDefinition

    /** The definition as it will be saved: blocks emptied by deleting their steps are dropped. */
    val cleaned: TimerDefinition get() = definition.removeEmptyBlocks()

    val validationError: TimerDefinition.ValidationError?
        get() = try {
            cleaned.validate()
            null
        } catch (error: TimerDefinition.ValidationError) {
            error
        }

    val canSave: Boolean get() = validationError == null

    val canAddBlock: Boolean get() = definition.blocks.size < Limits.maxBlocks

    fun canAddStep(blockId: UUID): Boolean =
        (definition.blocks.firstOrNull { it.id == blockId }?.steps?.size ?: Limits.maxStepsPerBlock) <
            Limits.maxStepsPerBlock

    fun withName(value: String): EditorState = copy(name = value.cappedName)

    fun withSets(value: Int): EditorState = changeDefinition { it.copy(sets = value.coerceIn(Limits.repeats)) }

    fun withSkipLastRest(value: Boolean): EditorState = changeDefinition { it.copy(skipLastRest = value) }

    fun withRepeats(blockId: UUID, value: Int): EditorState =
        changeBlock(blockId) { it.copy(repeats = value.coerceIn(Limits.repeats)) }

    /** A new block starts as work followed by rest. */
    fun addBlock(names: StepNames): EditorState {
        if (!canAddBlock) return this
        val work = Step.next(null, names::forType)
        val block = Block(steps = listOf(work, Step.next(work, names::forType)))
        return changeDefinition { it.copy(blocks = it.blocks + block) }
    }

    /** The copy gets new ids and sits right after the original. */
    fun duplicateBlock(blockId: UUID): EditorState {
        val index = definition.blocks.indexOfFirst { it.id == blockId }
        if (index < 0 || !canAddBlock) return this
        val original = definition.blocks[index]
        val copy = Block(
            steps = original.steps.map { Step(name = it.name, duration = it.duration, type = it.type) },
            repeats = original.repeats,
        )
        return changeDefinition { it.copy(blocks = it.blocks.take(index + 1) + copy + it.blocks.drop(index + 1)) }
    }

    fun deleteBlock(blockId: UUID): EditorState = changeDefinition { current ->
        current.copy(blocks = current.blocks.filterNot { it.id == blockId })
    }.copy(selection = selection - stepIdsOf(blockId))

    /** The step a "Add Step" tap offers: rest after work, work after anything else. */
    fun newStepFor(blockId: UUID, names: StepNames): Step =
        Step.next(definition.blocks.firstOrNull { it.id == blockId }?.steps?.lastOrNull(), names::forType)

    /** Saves a step from the sheet into its slot; a new block step is appended unless the block is full. */
    fun applyStep(slot: StepSlot, step: Step): EditorState = when (slot) {
        StepSlot.Prepare -> changeDefinition { it.copy(prepare = step) }
        StepSlot.RestBetweenSets -> changeDefinition { it.copy(restBetweenSets = step) }
        StepSlot.Cooldown -> changeDefinition { it.copy(cooldown = step) }
        is StepSlot.InBlock -> changeBlock(slot.blockId) { block ->
            val index = block.steps.indexOfFirst { it.id == slot.stepId }
            when {
                index >= 0 -> block.copy(steps = block.steps.replaced(index, step))
                slot.stepId == null && block.steps.size < Limits.maxStepsPerBlock ->
                    block.copy(steps = block.steps + step)
                else -> block
            }
        }
    }

    fun deleteStep(slot: StepSlot): EditorState = when (slot) {
        StepSlot.Prepare -> changeDefinition { it.copy(prepare = null) }
        StepSlot.RestBetweenSets -> changeDefinition { it.copy(restBetweenSets = null) }
        StepSlot.Cooldown -> changeDefinition { it.copy(cooldown = null) }
        is StepSlot.InBlock -> changeBlock(slot.blockId) { block ->
            block.copy(steps = block.steps.filterNot { it.id == slot.stepId })
        }.copy(selection = selection - setOfNotNull(slot.stepId))
    }

    /** Reorders inside one block, as iOS does: steps do not cross blocks. */
    fun moveStep(blockId: UUID, from: Int, to: Int): EditorState = changeBlock(blockId) { block ->
        if (from !in block.steps.indices || to !in block.steps.indices || from == to) {
            block
        } else {
            block.copy(steps = block.steps.moved(from, to))
        }
    }

    /** The block a step belongs to, or null. */
    fun blockOf(stepId: UUID): UUID? = definition.blocks.firstOrNull { block -> block.steps.any { it.id == stepId } }?.id

    /** Moves a step to the place of another step of the same block, for drag and drop. */
    fun moveStep(stepId: UUID, targetStepId: UUID): EditorState {
        val blockId = blockOf(stepId)
        if (blockId == null || blockId != blockOf(targetStepId)) return this
        val steps = definition.blocks.first { it.id == blockId }.steps
        return moveStep(blockId, steps.indexOfFirst { it.id == stepId }, steps.indexOfFirst { it.id == targetStepId })
    }

    fun startSelecting(): EditorState = copy(selecting = true, selection = emptySet())

    fun stopSelecting(): EditorState = copy(selecting = false, selection = emptySet())

    fun toggleSelected(stepId: UUID): EditorState =
        copy(selection = if (stepId in selection) selection - stepId else selection + stepId)

    /** On success, selection mode ends (iOS leaves the edit mode after a merge). */
    fun mergeSelection(): MergeResult = try {
        MergeResult.Merged(changeDefinition { it.mergeIntoBlock(selection) }.stopSelecting())
    } catch (error: TimerDefinition.MergeError) {
        MergeResult.Failed(error)
    }

    private fun stepIdsOf(blockId: UUID): Set<UUID> =
        definition.blocks.firstOrNull { it.id == blockId }?.steps?.map { it.id }?.toSet().orEmpty()

    private fun changeDefinition(change: (TimerDefinition) -> TimerDefinition): EditorState =
        copy(definition = change(definition))

    private fun changeBlock(blockId: UUID, change: (Block) -> Block): EditorState = changeDefinition { current ->
        current.copy(blocks = current.blocks.map { if (it.id == blockId) change(it) else it })
    }

    companion object {
        /** A new timer starts with prepare and one block of 30 s work and 15 s rest, three cycles. */
        fun newTimer(names: StepNames): EditorState {
            val definition = TimerDefinition(
                prepare = newStep(StepSlot.Prepare, names),
                blocks = listOf(
                    Block(
                        steps = listOf(
                            Step(name = names.work, duration = 30, type = StepType.WorkA),
                            Step(name = names.rest, duration = 15, type = StepType.Rest),
                        ),
                        repeats = 3,
                    ),
                ),
            )
            return EditorState(name = "", definition = definition, originalName = "", originalDefinition = definition)
        }

        fun existing(name: String, definition: TimerDefinition) =
            EditorState(name = name, definition = definition, originalName = name, originalDefinition = definition)

        /** A step created by an "Add ..." row. */
        fun newStep(slot: StepSlot, names: StepNames): Step = when (slot) {
            StepSlot.Prepare -> Step(name = names.prepare, duration = 10, type = StepType.Prepare)
            StepSlot.RestBetweenSets -> Step(name = names.restBetweenSets, duration = 60, type = StepType.Rest)
            StepSlot.Cooldown -> Step(name = names.cooldown, duration = 60, type = StepType.Cooldown)
            is StepSlot.InBlock -> Step(name = names.work, duration = 20, type = StepType.WorkA)
        }
    }
}

fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private fun <T> List<T>.replaced(index: Int, value: T): List<T> =
    toMutableList().apply { this[index] = value }
