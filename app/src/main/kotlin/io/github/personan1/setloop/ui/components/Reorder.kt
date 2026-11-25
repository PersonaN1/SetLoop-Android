package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Drag-and-drop reordering for a `LazyColumn` whose items have stable keys. The caller reorders its data in
 * `onMove` while the finger moves; the dragged item follows the finger through a translation, the others
 * animate into place. Dragging near the top or bottom edge scrolls the list.
 */
@Stable
class ReorderState internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val edgePx: Float,
    private val onMove: (fromKey: Any, toKey: Any) -> Unit,
    private val canDrop: (fromKey: Any, toKey: Any) -> Boolean,
    private val onDragFinished: () -> Unit,
) {
    var draggedKey: Any? by mutableStateOf(null)
        private set

    private var startOffset = 0f
    private var fingerDelta by mutableFloatStateOf(0f)

    /** Layout offset of the dragged item just before a swap; the next swap waits until the layout changes. */
    private var offsetBeforeSwap: Int? = null
    private var autoScroll: Job? = null

    private fun itemInfo(key: Any) = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    /** How far the dragged item is drawn from its layout position. */
    internal fun translationOf(key: Any): Float {
        if (key != draggedKey) return 0f
        val item = itemInfo(key) ?: return 0f
        return startOffset + fingerDelta - item.offset
    }

    internal fun start(key: Any) {
        val item = itemInfo(key) ?: return
        draggedKey = key
        startOffset = item.offset.toFloat()
        fingerDelta = 0f
        offsetBeforeSwap = null
        autoScroll = scope.launch { autoScrollLoop() }
    }

    internal fun drag(delta: Float) {
        fingerDelta += delta
        swapIfOverTarget()
    }

    internal fun finish() {
        if (draggedKey == null) return
        autoScroll?.cancel()
        draggedKey = null
        fingerDelta = 0f
        onDragFinished()
    }

    private fun swapIfOverTarget() {
        val key = draggedKey ?: return
        val dragged = itemInfo(key) ?: return
        if (offsetBeforeSwap == dragged.offset) return
        offsetBeforeSwap = null
        val center = startOffset + fingerDelta + dragged.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != key && center >= it.offset && center < it.offset + it.size && canDrop(key, it.key)
        } ?: return
        offsetBeforeSwap = dragged.offset
        onMove(key, target.key)
    }

    private suspend fun autoScrollLoop() {
        while (draggedKey != null) {
            val key = draggedKey ?: return
            val dragged = itemInfo(key)
            if (dragged != null) {
                val top = startOffset + fingerDelta
                val bottom = top + dragged.size
                val layout = listState.layoutInfo
                val step = when {
                    top < layout.viewportStartOffset + edgePx -> -SCROLL_STEP_PX
                    bottom > layout.viewportEndOffset - edgePx -> SCROLL_STEP_PX
                    else -> 0f
                }
                if (step != 0f) {
                    listState.scrollBy(step)
                    swapIfOverTarget()
                }
            }
            withFrameNanos { }
        }
    }

    private companion object {
        const val SCROLL_STEP_PX = 24f
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    onMove: (fromKey: Any, toKey: Any) -> Unit,
    onDragFinished: () -> Unit,
    canDrop: (fromKey: Any, toKey: Any) -> Boolean = { _, _ -> true },
): ReorderState {
    val scope = rememberCoroutineScope()
    val edgePx = with(LocalDensity.current) { 72.dp.toPx() }
    val currentOnMove by rememberUpdatedState(onMove)
    val currentCanDrop by rememberUpdatedState(canDrop)
    val currentOnFinished by rememberUpdatedState(onDragFinished)
    return remember(listState, scope, edgePx) {
        ReorderState(
            listState = listState,
            scope = scope,
            edgePx = edgePx,
            onMove = { from, to -> currentOnMove(from, to) },
            canDrop = { from, to -> currentCanDrop(from, to) },
            onDragFinished = { currentOnFinished() },
        )
    }
}

/** Put on the item row modifier: the dragged item floats above the rest, the others animate placement. */
@Suppress("ModifierFactoryExtensionFunction") // Needs the item scope for `animateItem`.
fun LazyItemScope.reorderableItem(state: ReorderState, key: Any): Modifier {
    if (state.draggedKey != key) return Modifier.animateItem()
    return Modifier
        .zIndex(1f)
        .graphicsLayer { translationY = state.translationOf(key) }
        .shadow(elevation = 6.dp)
}

/** Put on the drag handle of the item with [key]: dragging starts at once, without a long press. */
@Composable
fun Modifier.reorderHandle(state: ReorderState, key: Any): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(state, key) {
        detectDragGestures(
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                state.start(key)
            },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = state::finish,
            onDragCancel = state::finish,
        )
    }
}
