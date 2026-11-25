package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ListSelectionTest {
    private val ids = listOf("a", "b", "c")

    @Test
    fun `selection starts empty and toggles`() {
        val selection = ListSelection().start()
        assertTrue(selection.active)
        assertEquals(setOf("a"), selection.toggle("a").selected)
        assertTrue(selection.toggle("a").toggle("a").selected.isEmpty())
    }

    @Test
    fun `select all selects everything and then clears`() {
        val all = ListSelection().start().toggleAll(ids)
        assertEquals(ids.toSet(), all.selected)
        assertTrue(all.allSelected(ids))
        assertTrue(all.toggleAll(ids).selected.isEmpty())
        assertEquals(ids.toSet(), all.toggle("a").toggleAll(ids).selected)
    }

    @Test
    fun `all selected is false for an empty list`() {
        assertFalse(ListSelection().start().allSelected(emptyList()))
    }

    @Test
    fun `finish leaves selection mode and clears`() {
        val finished = ListSelection().start().toggle("a").finish()
        assertFalse(finished.active)
        assertTrue(finished.selected.isEmpty())
    }

    @Test
    fun `retain drops deleted timers`() {
        val selection = ListSelection().start().toggleAll(ids)
        assertEquals(setOf("a", "c"), selection.retain(listOf("a", "c")).selected)
    }

    @Test
    fun `after deleting some timers selection mode stays with nothing selected`() {
        val after = ListSelection().start().toggle("a").afterDelete(listOf("b", "c"))
        assertTrue(after.active)
        assertTrue(after.selected.isEmpty())
    }

    @Test
    fun `after deleting every timer selection mode ends`() {
        assertFalse(ListSelection().start().toggleAll(ids).afterDelete(emptyList()).active)
    }
}

class ReorderLogicTest {
    @Test
    fun `moved puts the item at the target index`() {
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").moved(0, 2))
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, 0))
    }

    @Test
    fun `moved ignores bad or equal indexes`() {
        val list = listOf("a", "b", "c")
        assertSame(list, list.moved(1, 1))
        assertSame(list, list.moved(-1, 1))
        assertSame(list, list.moved(0, 3))
    }

    @Test
    fun `in order follows the order and puts unlisted items last`() {
        val items = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "a", "b", "d"), items.inOrder(listOf("c", "a", "b")) { it })
        assertEquals(listOf("c", "a", "b", "d"), items.inOrder(listOf("c", "a", "b", "gone")) { it })
        assertSame(items, items.inOrder(null) { it })
    }
}

class StructureRowsTest {
    private val prepare = step("Prepare", 10, StepType.Prepare)
    private val rest = step("Rest", 60, StepType.Rest)
    private val cooldown = step("Cooldown", 60, StepType.Cooldown)

    @Test
    fun `rows follow the preview structure`() {
        val definition = TimerDefinition(
            prepare = prepare,
            blocks = listOf(Block(steps = listOf(step("A"), step("B")), repeats = 2), Block(steps = listOf(step("C")))),
            sets = 3,
            restBetweenSets = rest,
            cooldown = cooldown,
        )
        val rows = definition.structureRows()

        assertEquals(8, rows.size)
        assertEquals(prepare, assertIs<StructureRow.StepRow>(rows[0]).step)
        assertEquals(StructureRow.SetsRow(3, rest), rows[1])
        assertEquals(StructureRow.BlockRow(1, 2), rows[2])
        assertTrue(assertIs<StructureRow.StepRow>(rows[3]).indented)
        assertEquals(StructureRow.BlockRow(2, 1), rows[5])
        assertFalse(assertIs<StructureRow.StepRow>(rows.last()).indented)
    }

    @Test
    fun `one set has no sets row`() {
        val rows = TimerDefinition(blocks = listOf(Block(steps = listOf(step("A"))))).structureRows()
        assertEquals(2, rows.size)
        assertIs<StructureRow.BlockRow>(rows.first())
    }
}

class EditorRowsTest {
    @Test
    fun `row keys are unique and fit a bundle`() {
        val first = Block(steps = listOf(step("A"), step("B")))
        val second = Block(steps = listOf(step("C")))
        val state = editorOf(first, second)
            .applyStep(StepSlot.Cooldown, EditorState.newStep(StepSlot.Cooldown, testNames))
            .withSets(2)
        val rows = state.generalRows() + blockRows(first) + blockRows(second) + EditorRow.AddBlock + state.finishRows()

        val keys = rows.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        // Lazy list keys are saved in a Bundle: only strings and UUIDs are used.
        assertTrue(keys.all { it is String || it is UUID })
    }

    @Test
    fun `rest between sets row appears only for several sets`() {
        val state = editorOf(Block(steps = listOf(step("A"))))
        assertEquals(2, state.generalRows().size)
        assertEquals(3, state.withSets(2).generalRows().size)
    }
}
