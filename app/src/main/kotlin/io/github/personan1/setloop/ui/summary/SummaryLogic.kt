package io.github.personan1.setloop.ui.summary

import androidx.compose.runtime.saveable.listSaver
import io.github.personan1.setloop.core.cappedName
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** The two fields of the summary screen. */
data class SummaryFields(val name: String, val note: String = "") {
    /** True when something typed would be lost on "Don't Save". */
    fun isEdited(timerName: String): Boolean = name != timerName || note.isNotBlank()

    fun withName(value: String): SummaryFields = copy(name = value.cappedName)
}

/**
 * Save and discard of one finished workout. A second tap on Save while the first is running does nothing,
 * so a double tap can't insert the record twice (iOS `isSaved`).
 */
class SummarySaveFlow(
    private val persist: suspend (name: String, note: String) -> Unit,
    private val dismiss: () -> Unit,
) {
    private val saved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = saved.asStateFlow()

    /** Leaving the screen mid-save must not drop the record, so the write runs to the end. */
    suspend fun save(fields: SummaryFields) {
        if (!saved.compareAndSet(expect = false, update = true)) return
        withContext(NonCancellable) {
            persist(fields.name, fields.note)
            dismiss()
        }
    }

    fun discard() {
        if (!saved.value) dismiss()
    }
}

val SummaryFieldsSaver = listSaver<SummaryFields, String>(
    save = { listOf(it.name, it.note) },
    restore = { SummaryFields(it[0], it[1]) },
)
