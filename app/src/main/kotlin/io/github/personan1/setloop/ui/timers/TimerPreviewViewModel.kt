package io.github.personan1.setloop.ui.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.personan1.setloop.data.TimerAddResult
import io.github.personan1.setloop.data.TimerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimerPreviewState {
    data object Loading : TimerPreviewState

    /** Deleted, here or elsewhere: the screen closes. */
    data object Missing : TimerPreviewState

    data class Loaded(val timer: TimerItem, val limitReached: Boolean) : TimerPreviewState
}

class TimerPreviewViewModel(private val timerId: String, private val repository: TimerRepository) : ViewModel() {
    private val limitReached = MutableStateFlow(false)

    val state: StateFlow<TimerPreviewState> = combine(
        repository.observeTimers().map { timers -> timers.firstOrNull { it.id == timerId } },
        limitReached,
    ) { entity, limit ->
        if (entity == null) TimerPreviewState.Missing else TimerPreviewState.Loaded(TimerItem(entity), limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TimerPreviewState.Loading)

    fun duplicate() {
        viewModelScope.launch {
            if (repository.duplicate(timerId) is TimerAddResult.LimitReached) limitReached.value = true
        }
    }

    fun delete() {
        viewModelScope.launch { repository.deleteMany(listOf(timerId)) }
    }

    fun dismissLimit() {
        limitReached.value = false
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
