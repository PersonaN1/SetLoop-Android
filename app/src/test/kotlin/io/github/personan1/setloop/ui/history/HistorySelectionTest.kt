package io.github.personan1.setloop.ui.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HistorySelectionTest {
    private val all = listOf("a", "b", "c")

    @Test
    fun `toggle works only while selecting`() {
        val idle = HistorySelection()
        assertSame(idle, idle.toggle("a"))

        val selecting = idle.start().toggle("a").toggle("b").toggle("a")
        assertEquals(setOf("b"), selecting.selected)
        assertEquals(1, selecting.count)
    }

    @Test
    fun `long press starts with the record ticked`() {
        val selection = HistorySelection().start("b")

        assertTrue(selection.isSelecting)
        assertEquals(setOf("b"), selection.selected)
    }

    @Test
    fun `select all ticks everything, then clears`() {
        val selection = HistorySelection().start().toggle("a")

        val ticked = selection.toggleAll(all)
        assertEquals(all.toSet(), ticked.selected)
        assertTrue(ticked.allSelected(all))

        val cleared = ticked.toggleAll(all)
        assertEquals(emptySet(), cleared.selected)
        assertFalse(cleared.allSelected(all))
    }

    @Test
    fun `an empty list is never all selected`() {
        assertFalse(HistorySelection().start().allSelected(emptyList()))
    }

    @Test
    fun `finish leaves selection mode and forgets ticks`() {
        assertEquals(HistorySelection(), HistorySelection().start("a").finish())
    }

    @Test
    fun `retain drops deleted ids and keeps the mode`() {
        val selection = HistorySelection().start().toggleAll(all)

        val after = selection.retain(listOf("a", "c"))

        assertTrue(after.isSelecting)
        assertEquals(setOf("a", "c"), after.selected)
    }

    @Test
    fun `retain on an empty list ends selection mode`() {
        assertEquals(HistorySelection(), HistorySelection().start("a").retain(emptyList()))
    }

    @Test
    fun `retain does nothing outside selection mode`() {
        val idle = HistorySelection()

        assertSame(idle, idle.retain(emptyList()))
    }
}
