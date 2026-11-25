package io.github.personan1.setloop.ui.timers

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.data.TimerAddResult
import io.github.personan1.setloop.data.TimerRepository
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class EditorUiState(
    /** Null while an existing timer loads. */
    val editor: EditorState? = null,
    val stepEdit: StepEdit? = null,
    val mergeError: TimerDefinition.MergeError? = null,
    val limitReached: Boolean = false,
    val saving: Boolean = false,
)

/**
 * Holds the editor's working copy across rotation. [timerId] is null for a new timer. [defaultName] is the
 * name an empty name field gets, numbered by the repository.
 */
class TimerEditorViewModel(
    private val timerId: String?,
    private val repository: TimerRepository,
    private val names: StepNames,
    private val defaultName: String,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState(editor = if (timerId == null) EditorState.newTimer(names) else null))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val close = Channel<Unit>(Channel.BUFFERED)

    /** Emits when the screen should close: the timer was saved, or no longer exists. */
    val closeEvents = close.receiveAsFlow()

    init {
        if (timerId != null) {
            viewModelScope.launch {
                val timer = repository.get(timerId)
                if (timer == null) {
                    close.send(Unit)
                } else {
                    _state.update { it.copy(editor = EditorState.existing(timer.name, timer.definition)) }
                }
            }
        }
    }

    fun setName(name: String) = edit { it.withName(name) }

    fun setSets(value: Int) = edit { it.withSets(value) }

    fun setSkipLastRest(value: Boolean) = edit { it.withSkipLastRest(value) }

    fun setRepeats(blockId: UUID, value: Int) = edit { it.withRepeats(blockId, value) }

    fun addBlock() = edit { it.addBlock(names) }

    fun duplicateBlock(blockId: UUID) = edit { it.duplicateBlock(blockId) }

    fun deleteBlock(blockId: UUID) = edit { it.deleteBlock(blockId) }

    fun moveStep(stepId: UUID, targetStepId: UUID) = edit { it.moveStep(stepId, targetStepId) }

    fun moveStepBy(blockId: UUID, index: Int, offset: Int) = edit { it.moveStep(blockId, index, index + offset) }

    fun startSelecting() = edit { it.startSelecting() }

    fun stopSelecting() = edit { it.stopSelecting() }

    fun toggleSelected(stepId: UUID) = edit { it.toggleSelected(stepId) }

    fun merge() {
        val editor = _state.value.editor ?: return
        when (val result = editor.mergeSelection()) {
            is MergeResult.Merged -> edit { result.state }
            is MergeResult.Failed -> _state.update { it.copy(mergeError = result.error) }
        }
    }

    fun dismissMergeError() = _state.update { it.copy(mergeError = null) }

    fun dismissLimit() = _state.update { it.copy(limitReached = false) }

    /** Opens the step sheet for an existing step or for the "Add ..." row of [slot]. */
    fun openStep(slot: StepSlot, step: Step? = null) {
        val editor = _state.value.editor ?: return
        val edit = if (step != null) {
            StepEdit(slot, step)
        } else {
            val fresh = when (slot) {
                is StepSlot.InBlock -> editor.newStepFor(slot.blockId, names)
                else -> EditorState.newStep(slot, names)
            }
            StepEdit(slot, fresh, isNew = true)
        }
        _state.update { it.copy(stepEdit = edit) }
    }

    fun closeStepSheet() = _state.update { it.copy(stepEdit = null) }

    fun saveStep(step: Step) {
        val slot = _state.value.stepEdit?.slot ?: return
        _state.update { it.copy(stepEdit = null) }
        edit { it.applyStep(slot, step) }
    }

    fun deleteStep() {
        val slot = _state.value.stepEdit?.slot ?: return
        _state.update { it.copy(stepEdit = null) }
        edit { it.deleteStep(slot) }
    }

    /** Done: writes the cleaned copy through the repository. Does nothing while the copy is invalid. */
    fun save() {
        val current = _state.value
        val editor = current.editor ?: return
        if (!editor.canSave || current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = if (timerId == null) {
                when (repository.add(editor.name, editor.cleaned, defaultName)) {
                    is TimerAddResult.Added -> true
                    TimerAddResult.LimitReached -> {
                        _state.update { it.copy(limitReached = true) }
                        false
                    }
                    TimerAddResult.NotFound -> false
                }
            } else {
                // Null: the timer was deleted meanwhile, so there is nothing left to edit.
                repository.update(timerId, editor.name, editor.cleaned, defaultName) != null
            }
            if (saved || timerId != null) close.send(Unit)
            _state.update { it.copy(saving = false) }
        }
    }

    private fun edit(change: (EditorState) -> EditorState) = _state.update { current ->
        val editor = current.editor ?: return@update current
        current.copy(editor = change(editor))
    }
}
