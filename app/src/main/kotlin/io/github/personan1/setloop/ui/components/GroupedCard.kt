package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val GroupRadius = 20.dp

/** Corners of one row in a group of rows that look like one rounded card (iOS inset grouped list). */
fun groupedShape(first: Boolean, last: Boolean) = RoundedCornerShape(
    topStart = if (first) GroupRadius else 0.dp,
    topEnd = if (first) GroupRadius else 0.dp,
    bottomStart = if (last) GroupRadius else 0.dp,
    bottomEnd = if (last) GroupRadius else 0.dp,
)

/**
 * One row of a grouped card. Each row is its own lazy-list item, so rows can be reordered, yet the group
 * reads as a single card: the first row has the top corners, the last the bottom ones, a divider sits between.
 */
@Composable
fun GroupedRow(
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.clip(groupedShape(first, last)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column {
            content()
            if (!last) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
