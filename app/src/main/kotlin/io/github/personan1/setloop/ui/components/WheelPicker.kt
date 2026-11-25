package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val RowHeight = 32.dp
private const val VISIBLE_ROWS = 5

/**
 * A vertical wheel of [count] values that snaps to a row; the middle row is selected. [label] names a
 * value on screen, [description] is what a screen reader says for the wheel, which it can also adjust.
 */
@Composable
fun WheelPicker(
    count: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    label: (Int) -> String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }
    val centered by remember(listState, rowPx) { derivedStateOf { listState.centeredIndex(rowPx, count) } }
    val currentOnSelected by rememberUpdatedState(onSelected)

    // The wheel reports changes of the middle row while the user scrolls; a value changed from outside moves the wheel.
    LaunchedEffect(listState) {
        snapshotFlow { centered to listState.isScrollInProgress }.collect { (index, scrolling) ->
            if (scrolling) currentOnSelected(index)
        }
    }
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress && centered != selectedIndex) listState.scrollToItem(selectedIndex)
    }

    Box(
        modifier = modifier
            .height(RowHeight * VISIBLE_ROWS)
            .semantics {
                contentDescription = description
                stateDescription = label(selectedIndex)
                progressBarRangeInfo = ProgressBarRangeInfo(selectedIndex.toFloat(), 0f..(count - 1).toFloat(), count - 2)
                setProgress { value ->
                    onSelected(value.roundToInt().coerceIn(0, count - 1))
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = RowHeight * (VISIBLE_ROWS / 2)),
            modifier = Modifier.clearAndSetSemantics {},
        ) {
            itemsIndexed(List(count) { it }) { index, value ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(RowHeight).alpha(rowAlpha(abs(index - centered))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = label(value), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private fun rowAlpha(distance: Int): Float = when (distance) {
    0 -> 1f
    1 -> 0.6f
    else -> 0.3f
}

/** With the padding of two rows, the middle row is the first visible one, or the next when scrolled past half. */
private fun LazyListState.centeredIndex(rowPx: Float, count: Int): Int {
    val passedHalf = firstVisibleItemScrollOffset > rowPx / 2
    return (firstVisibleItemIndex + if (passedHalf) 1 else 0).coerceIn(0, count - 1)
}
