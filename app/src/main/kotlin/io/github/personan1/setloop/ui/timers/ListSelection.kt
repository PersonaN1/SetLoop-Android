package io.github.personan1.setloop.ui.timers

/** Selection mode of the timer list (DESIGN V6, SPEC D9): Select, Select All, Delete (N). */
data class ListSelection(val active: Boolean = false, val selected: Set<String> = emptySet()) {
    fun start() = ListSelection(active = true)

    fun finish() = ListSelection()

    fun toggle(id: String) = copy(selected = if (id in selected) selected - id else selected + id)

    fun allSelected(allIds: List<String>) = allIds.isNotEmpty() && selected.containsAll(allIds)

    fun toggleAll(allIds: List<String>) = copy(selected = if (allSelected(allIds)) emptySet() else allIds.toSet())

    /** Drops timers that no longer exist. */
    fun retain(allIds: List<String>) = copy(selected = selected.intersect(allIds.toSet()))

    /** After deleting, selection mode ends when no timer is left to select. */
    fun afterDelete(remainingIds: List<String>) = if (remainingIds.isEmpty()) finish() else copy(selected = emptySet())
        .retain(remainingIds)
}

/** Items in the order of [order]; items it does not list (added meanwhile) keep their order after the listed ones. */
fun <T> List<T>.inOrder(order: List<String>?, id: (T) -> String): List<T> {
    if (order == null) return this
    val position = order.withIndex().associate { (index, key) -> key to index }
    return sortedBy { position[id(it)] ?: Int.MAX_VALUE }
}
