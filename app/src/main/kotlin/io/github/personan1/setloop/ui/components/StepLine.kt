package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.core.StepType

/** Minimum height of a list row (DESIGN V7). */
val MinRowHeight = 44.dp

/**
 * A step name with its color mark and a trailing detail (usually its time). At large font scale the
 * detail goes under the name, so the name is not broken mid-word.
 */
@Composable
fun StepLine(
    name: String,
    modifier: Modifier = Modifier,
    mark: @Composable () -> Unit = {},
    detail: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = MinRowHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mark()
        if (isLargeFontScale) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically, content = detail)
            }
        } else {
            Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            detail()
        }
    }
}

/** A read-only step row: type dot, name, duration in secondary color read out in words. */
@Composable
fun StepLine(name: String, type: StepType, seconds: Int, modifier: Modifier = Modifier) {
    StepLine(
        name = name,
        modifier = modifier,
        mark = { StepTypeDot(type) },
        detail = {
            DurationText(
                seconds = seconds,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
    )
}
