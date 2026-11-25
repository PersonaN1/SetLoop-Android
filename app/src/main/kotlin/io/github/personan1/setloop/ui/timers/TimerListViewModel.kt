package io.github.personan1.setloop.ui.timers

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.data.SavedTimerEntity
import io.github.personan1.setloop.data.TimerAddResult
import io.github.personan1.setloop.data.TimerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A saved timer ready for a list card; the definition is decoded once, not on every recomposition. */
@Immutable
data class TimerItem(val id: String, val name: String, val totalDuration: Int, val definition: TimerDefinition) {
    constructor(entity: SavedTimerEntity) : this(entity.id, entity.name, entity.totalDuration, entity.definition)
}

@Immutable
data class TimerListUiState(
    /** Null until the first load. */
    val timers: List<TimerItem>? = null,
    val selection: ListSelection = ListSelection(),
    val limitReached: Boolean = false,
)

class TimerListViewModel(private val repository: TimerRepository) : ViewModel() {
    private val selection = MutableStateFlow(ListSelection())
    private val limitReached = MutableStateFlow(false)

    /** Order shown while dragging and until the database reports it; null shows the stored order. */
    private val dragOrder = MutableStateFlow<List<String>?>(null)
    private var storedIds: List<String> = emptyList()
    private var dragging = false

    val state: StateFlow<TimerListUiState> = combine(
        repository.observeTimers().map { list -> list.map(::TimerItem) },
        dragOrder,
        selection,
        limitReached,
    ) { timers, order, selection, limit ->
        storedIds = timers.map { it.id }
        // The stored order caught up with the dragged one: stop overriding it.
        if (order != null && order == storedIds && !dragging) dragOrder.value = null
        TimerListUiState(
            timers = timers.inOrder(order) { it.id },
            selection = if (selection.active) selection.retain(storedIds) else selection,
            limitReached = limit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TimerListUiState())

    fun startSelecting() = selection.update { it.start() }

    fun finishSelecting() = selection.update { it.finish() }

    fun toggleSelected(id: String) = selection.update { it.toggle(id) }

    fun toggleAll() = selection.update { it.toggleAll(storedIds) }

    fun deleteSelected() {
        val ids = selection.value.selected
        viewModelScope.launch {
            repository.deleteMany(ids)
            selection.update { it.afterDelete(storedIds - ids) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteMany(listOf(id)) }
    }

    fun duplicate(id: String) {
        viewModelScope.launch {
            if (repository.duplicate(id) is TimerAddResult.LimitReached) limitReached.value = true
        }
    }

    /** "+" tap: refuses at the limit without opening the editor. Returns whether the editor may open. */
    fun canAddTimer(): Boolean {
        if (storedIds.size < Limits.maxTimers) return true
        limitReached.value = true
        return false
    }

    fun dismissLimit() {
        limitReached.value = false
    }

    /** Moves [id] to the place of [targetId] while dragging. */
    fun move(id: String, targetId: String) {
        dragging = true
        val current = dragOrder.value ?: storedIds
        dragOrder.value = current.moved(current.indexOf(id), current.indexOf(targetId))
    }

    fun moveBy(id: String, offset: Int) {
        val current = dragOrder.value ?: storedIds
        val from = current.indexOf(id)
        dragOrder.value = current.moved(from, from + offset)
        saveOrder()
    }

    /** Drag ended: stores the new order. */
    fun saveOrder() {
        dragging = false
        val order = dragOrder.value ?: return
        if (order == storedIds) {
            dragOrder.value = null
        } else {
            viewModelScope.launch { repository.reorder(order) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
