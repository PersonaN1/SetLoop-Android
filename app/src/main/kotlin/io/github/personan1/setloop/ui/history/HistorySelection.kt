package io.github.personan1.setloop.ui.history

import androidx.compose.runtime.saveable.listSaver

/** "Select" mode of the history list (iOS `SelectionToolbar`): which records are ticked. */
data class HistorySelection(
    val isSelecting: Boolean = false,
    val selected: Set<String> = emptySet(),
) {
    val count: Int get() = selected.size

    /** Enters selection mode, optionally with one record already ticked (long press). */
    fun start(first: String? = null) = HistorySelection(isSelecting = true, selected = setOfNotNull(first))

    fun finish() = HistorySelection()

    fun toggle(id: String): HistorySelection =
        if (isSelecting) copy(selected = if (id in selected) selected - id else selected + id) else this

    fun allSelected(allIds: Collection<String>): Boolean = allIds.isNotEmpty() && selected.containsAll(allIds)

    /** "Select All" ticks everything; when everything is ticked it becomes "Deselect All". */
    fun toggleAll(allIds: Collection<String>): HistorySelection =
        copy(selected = if (allSelected(allIds)) emptySet() else allIds.toSet())

    /**
     * Drops ids that no longer exist. Selection mode ends once the list is empty, as on iOS
     * after the last record was deleted.
     */
    fun retain(existing: Collection<String>): HistorySelection {
        if (!isSelecting) return this
        if (existing.isEmpty()) return finish()
        val kept = selected.intersect(existing.toSet())
        return if (kept.size == selected.size) this else copy(selected = kept)
    }

    companion object {
        val Saver = listSaver<HistorySelection, Any>(
            save = { listOf(it.isSelecting) + it.selected },
            restore = { saved -> HistorySelection(saved[0] as Boolean, saved.drop(1).map { it as String }.toSet()) },
        )
    }
}
