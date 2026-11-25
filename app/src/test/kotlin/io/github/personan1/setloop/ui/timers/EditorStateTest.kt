package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal val testNames = StepNames(
    prepare = "Prepare",
    work = "Work",
    rest = "Rest",
    restBetweenSets = "Rest between sets",
    cooldown = "Cooldown",
)

internal fun step(name: String, seconds: Int = 20, type: StepType = StepType.WorkA) =
    Step(name = name, duration = seconds, type = type)

internal fun editorOf(vararg blocks: Block) = EditorState.existing(
    name = "Timer",
    definition = TimerDefinition(blocks = blocks.toList()),
)

class EditorStateTest {
    private val newTimer = EditorState.newTimer(testNames)

    @Test
    fun `new timer starts with prepare and one block of work and rest`() {
        val definition = newTimer.definition
        assertEquals(StepType.Prepare, definition.prepare?.type)
        assertEquals(1, definition.blocks.size)
        assertEquals(listOf(30, 15), definition.blocks.single().steps.map { it.duration })
        assertEquals(3, definition.blocks.single().repeats)
        assertFalse(newTimer.hasChanges)
        assertTrue(newTimer.canSave)
    }

    @Test
    fun `dirty tracking follows the difference to the original`() {
        val changed = newTimer.withSets(2)
        assertTrue(changed.hasChanges)
        assertFalse(changed.withSets(1).hasChanges)
        assertTrue(newTimer.withName("Run").hasChanges)
        assertFalse(newTimer.withName("Run").withName("").hasChanges)
    }

    @Test
    fun `sets and cycles are kept in range`() {
        val blockId = newTimer.definition.blocks.single().id
        assertEquals(1, newTimer.withSets(0).definition.sets)
        assertEquals(Limits.repeats.last, newTimer.withSets(500).definition.sets)
        assertEquals(1, newTimer.withRepeats(blockId, -3).definition.blocks.single().repeats)
        assertEquals(99, newTimer.withRepeats(blockId, 100).definition.blocks.single().repeats)
    }

    @Test
    fun `name is capped`() {
        assertEquals(Limits.maxNameLength, newTimer.withName("x".repeat(100)).name.length)
    }

    @Test
    fun `add block appends work then rest`() {
        val result = newTimer.addBlock(testNames)
        val added = result.definition.blocks.last()
        assertEquals(2, result.definition.blocks.size)
        assertEquals(listOf(StepType.WorkA, StepType.Rest), added.steps.map { it.type })
        assertEquals(listOf("Work", "Rest"), added.steps.map { it.name })
        assertEquals(1, added.repeats)
    }

    @Test
    fun `add block stops at the limit`() {
        var state = newTimer
        repeat(Limits.maxBlocks) { state = state.addBlock(testNames) }
        assertEquals(Limits.maxBlocks, state.definition.blocks.size)
        assertFalse(state.canAddBlock)
        assertSame(state, state.addBlock(testNames))
        assertSame(state, state.duplicateBlock(state.definition.blocks.first().id))
    }

    @Test
    fun `duplicate block copies steps with new ids right after the original`() {
        val withTwo = newTimer.addBlock(testNames)
        val original = withTwo.definition.blocks.first()
        val result = withTwo.duplicateBlock(original.id)

        assertEquals(3, result.definition.blocks.size)
        val copy = result.definition.blocks[1]
        assertNotEquals(original.id, copy.id)
        assertEquals(original.repeats, copy.repeats)
        assertEquals(original.steps.map { it.name to it.duration }, copy.steps.map { it.name to it.duration })
        assertTrue(copy.steps.map { it.id }.intersect(original.steps.map { it.id }.toSet()).isEmpty())
        assertEquals(withTwo.definition.blocks.last().id, result.definition.blocks.last().id)
    }

    @Test
    fun `delete block removes it and its selected steps`() {
        val withTwo = newTimer.addBlock(testNames)
        val first = withTwo.definition.blocks.first()
        val state = withTwo.startSelecting().toggleSelected(first.steps.first().id).deleteBlock(first.id)
        assertEquals(1, state.definition.blocks.size)
        assertTrue(state.selection.isEmpty())
    }

    @Test
    fun `step limit per block is enforced for new steps`() {
        val full = Block(steps = List(Limits.maxStepsPerBlock) { step("S$it") })
        val state = editorOf(full)
        assertFalse(state.canAddStep(full.id))
        val result = state.applyStep(StepSlot.InBlock(full.id, null), step("Extra"))
        assertSame(state.definition.blocks.single().steps.size, result.definition.blocks.single().steps.size)

        val roomy = editorOf(Block(steps = listOf(step("A"))))
        assertTrue(roomy.canAddStep(roomy.definition.blocks.single().id))
    }

    @Test
    fun `apply step adds replaces and fills fixed slots`() {
        val block = Block(steps = listOf(step("A"), step("B")))
        val state = editorOf(block)

        val added = state.applyStep(StepSlot.InBlock(block.id, null), step("C"))
        assertEquals(listOf("A", "B", "C"), added.definition.blocks.single().steps.map { it.name })

        val replaced = state.applyStep(StepSlot.InBlock(block.id, block.steps[1].id), block.steps[1].copy(name = "B2"))
        assertEquals(listOf("A", "B2"), replaced.definition.blocks.single().steps.map { it.name })

        val prepared = state.applyStep(StepSlot.Prepare, EditorState.newStep(StepSlot.Prepare, testNames))
        assertEquals(10, prepared.definition.prepare?.duration)
        assertEquals(60, EditorState.newStep(StepSlot.Cooldown, testNames).duration)
        assertEquals(StepType.Rest, EditorState.newStep(StepSlot.RestBetweenSets, testNames).type)
    }

    @Test
    fun `delete step removes block steps and fixed slots`() {
        val block = Block(steps = listOf(step("A"), step("B")))
        val state = editorOf(block)
            .applyStep(StepSlot.Cooldown, EditorState.newStep(StepSlot.Cooldown, testNames))

        assertNull(state.deleteStep(StepSlot.Cooldown).definition.cooldown)
        val without = state.deleteStep(StepSlot.InBlock(block.id, block.steps[0].id))
        assertEquals(listOf("B"), without.definition.blocks.single().steps.map { it.name })
    }

    @Test
    fun `a block emptied by deleting its steps is dropped when saving`() {
        val first = Block(steps = listOf(step("A")))
        val second = Block(steps = listOf(step("B")))
        val state = editorOf(first, second).deleteStep(StepSlot.InBlock(first.id, first.steps.single().id))

        assertEquals(2, state.definition.blocks.size)
        assertEquals(listOf(second.id), state.cleaned.blocks.map { it.id })
        assertTrue(state.canSave)
    }

    @Test
    fun `no steps at all cannot be saved`() {
        val block = Block(steps = listOf(step("A")))
        val state = editorOf(block).deleteStep(StepSlot.InBlock(block.id, block.steps.single().id))
        assertIs<TimerDefinition.ValidationError.NoSteps>(state.validationError)
        assertFalse(state.canSave)
    }

    @Test
    fun `only a skipped last rest cannot be saved`() {
        val state = editorOf(Block(steps = listOf(step("Rest", type = StepType.Rest)))).withSkipLastRest(true)
        assertIs<TimerDefinition.ValidationError.EmptyPlan>(state.validationError)
        assertFalse(state.withSkipLastRest(false).validationError != null)
    }

    @Test
    fun `step duration out of range is a validation error`() {
        val block = Block(steps = listOf(step("A")))
        val state = editorOf(block).applyStep(
            StepSlot.InBlock(block.id, block.steps.single().id),
            block.steps.single().copy(duration = 0),
        )
        assertIs<TimerDefinition.ValidationError.StepDurationOutOfRange>(state.validationError)
    }

    @Test
    fun `move step reorders inside a block`() {
        val block = Block(steps = listOf(step("A"), step("B"), step("C")))
        val state = editorOf(block)
        val moved = state.moveStep(block.id, 0, 2)
        assertEquals(listOf("B", "C", "A"), moved.definition.blocks.single().steps.map { it.name })

        val byIds = state.moveStep(block.steps[2].id, block.steps[0].id)
        assertEquals(listOf("C", "A", "B"), byIds.definition.blocks.single().steps.map { it.name })

        assertEquals(state.definition, state.moveStep(block.id, 1, 1).definition)
        assertEquals(state.definition, state.moveStep(block.id, 0, 9).definition)
    }

    @Test
    fun `move step does not cross blocks`() {
        val first = Block(steps = listOf(step("A"), step("B")))
        val second = Block(steps = listOf(step("C")))
        val state = editorOf(first, second)
        assertEquals(state.definition, state.moveStep(first.steps[0].id, second.steps[0].id).definition)
        assertEquals(first.id, state.blockOf(first.steps[1].id))
        assertNull(state.blockOf(UUID.randomUUID()))
    }

    @Test
    fun `selection toggles and ends with selection mode`() {
        val block = Block(steps = listOf(step("A"), step("B")))
        val state = editorOf(block).startSelecting()
        assertTrue(state.selecting)
        val selected = state.toggleSelected(block.steps[0].id)
        assertEquals(setOf(block.steps[0].id), selected.selection)
        assertTrue(selected.toggleSelected(block.steps[0].id).selection.isEmpty())
        val stopped = selected.stopSelecting()
        assertFalse(stopped.selecting)
        assertTrue(stopped.selection.isEmpty())
    }

    @Test
    fun `merge splits a block and leaves selection mode`() {
        val block = Block(steps = listOf(step("A"), step("B"), step("C"), step("D")), repeats = 3)
        val state = EditorState.existing("Timer", TimerDefinition(blocks = listOf(block))).startSelecting()
            .toggleSelected(block.steps[1].id)
            .toggleSelected(block.steps[2].id)

        val merged = assertIs<MergeResult.Merged>(state.mergeSelection()).state
        assertEquals(listOf(listOf("A"), listOf("B", "C"), listOf("D")), merged.definition.blocks.map { b -> b.steps.map { it.name } })
        assertEquals(listOf(3, 2, 3), merged.definition.blocks.map { it.repeats })
        assertFalse(merged.selecting)
        assertTrue(merged.selection.isEmpty())
        assertTrue(merged.hasChanges)
    }

    @Test
    fun `merge reports why it failed and keeps the state`() {
        val block = Block(steps = listOf(step("A"), step("B"), step("C")))
        val other = Block(steps = listOf(step("D")))
        val base = editorOf(block, other).startSelecting()

        val empty = assertIs<MergeResult.Failed>(base.mergeSelection())
        assertIs<TimerDefinition.MergeError.NotContiguous>(empty.error)

        val gap = base.toggleSelected(block.steps[0].id).toggleSelected(block.steps[2].id)
        assertIs<TimerDefinition.MergeError.NotContiguous>(assertIs<MergeResult.Failed>(gap.mergeSelection()).error)

        val acrossBlocks = base.toggleSelected(block.steps[2].id).toggleSelected(other.steps[0].id)
        assertIs<TimerDefinition.MergeError.NotContiguous>(assertIs<MergeResult.Failed>(acrossBlocks.mergeSelection()).error)

        val whole = base.toggleSelected(other.steps[0].id)
        assertIs<TimerDefinition.MergeError.WholeBlock>(assertIs<MergeResult.Failed>(whole.mergeSelection()).error)
    }

    @Test
    fun `merge refuses to exceed the block limit`() {
        val blocks = Array(Limits.maxBlocks) { Block(steps = listOf(step("A$it"), step("B$it"))) }
        val state = editorOf(*blocks).startSelecting().toggleSelected(blocks[0].steps[0].id)
        assertIs<TimerDefinition.MergeError.TooManyBlocks>(assertIs<MergeResult.Failed>(state.mergeSelection()).error)
    }

    @Test
    fun `new step offers rest after work and work after rest`() {
        val block = Block(steps = listOf(step("A", type = StepType.WorkA)))
        val state = editorOf(block)
        val next = state.newStepFor(block.id, testNames)
        assertEquals(StepType.Rest, next.type)
        assertEquals("Rest", next.name)

        val afterRest = state.applyStep(StepSlot.InBlock(block.id, null), next)
        assertEquals(StepType.WorkA, afterRest.newStepFor(block.id, testNames).type)
    }
}
