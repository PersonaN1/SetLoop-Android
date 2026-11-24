package io.github.personan1.setloop.core

import java.util.UUID

/**
 * "Merge into Block" (SPEC D2): neighboring steps of one block become their own block, looped twice.
 * Steps before and after the selection stay in blocks of their own with the old repeat count.
 *
 * @throws TimerDefinition.MergeError
 */
fun TimerDefinition.mergeIntoBlock(stepIds: Set<UUID>): TimerDefinition {
    val blockIndex = blocks.indexOfFirst { block -> block.steps.any { it.id in stepIds } }
    if (blockIndex < 0) throw TimerDefinition.MergeError.NotContiguous
    val block = blocks[blockIndex]
    val positions = block.steps.indices.filter { block.steps[it].id in stepIds }
    val first = positions.firstOrNull()
    val last = positions.lastOrNull()
    if (positions.size != stepIds.size || first == null || last == null || last - first + 1 != positions.size) {
        throw TimerDefinition.MergeError.NotContiguous
    }
    if (positions.size >= block.steps.size) throw TimerDefinition.MergeError.WholeBlock

    val replacement = mutableListOf<Block>()
    if (first > 0) replacement.add(Block(steps = block.steps.subList(0, first), repeats = block.repeats))
    replacement.add(Block(steps = block.steps.subList(first, last + 1), repeats = 2))
    if (last < block.steps.size - 1) {
        replacement.add(Block(steps = block.steps.subList(last + 1, block.steps.size), repeats = block.repeats))
    }
    // The first piece stays the original block, so the list keeps its identity in place.
    replacement[0] = replacement[0].copy(id = block.id)
    if (blocks.size - 1 + replacement.size > Limits.maxBlocks) throw TimerDefinition.MergeError.TooManyBlocks
    return copy(blocks = blocks.subList(0, blockIndex) + replacement + blocks.subList(blockIndex + 1, blocks.size))
}

/** Drops blocks without steps, e.g. after the user deleted every step of one. */
fun TimerDefinition.removeEmptyBlocks(): TimerDefinition = copy(blocks = blocks.filter { it.steps.isNotEmpty() })

/** A sensible next step: rest after work, work after anything else. */
fun Step.Companion.next(previous: Step?, names: (StepType) -> String): Step {
    val type = if (previous?.type?.category == StepType.Category.Work) StepType.Rest else StepType.WorkA
    return Step(name = names(type), duration = if (type == StepType.Rest) 10 else 20, type = type)
}
