package io.github.personan1.setloop.ui.components

import android.content.res.Resources
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.stepCount
import io.github.personan1.setloop.workout.WorkoutFormat

/** "1 hour, 5 minutes, 30 seconds" for screen readers instead of "1:05:30". Zero reads "0 seconds". */
fun spokenDuration(resources: Resources, seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    val hours = total / 3600
    val minutes = total / 60 % 60
    val rest = total % 60
    val parts = buildList {
        if (hours > 0) add(resources.getQuantityString(R.plurals.duration_hours, hours, hours))
        if (minutes > 0) add(resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes))
        if (rest > 0 || total == 0) add(resources.getQuantityString(R.plurals.duration_seconds, rest, rest))
    }
    return parts.joinToString(resources.getString(R.string.duration_separator))
}

@Composable
fun spokenDuration(seconds: Int): String = spokenDuration(LocalResources.current, seconds)

/** "1 block", "2 blocks", plus "3 sets" when more than one and "33 steps" when asked (DESIGN V9). */
fun TimerDefinition.structureText(resources: Resources, includingSteps: Boolean = false): String {
    val parts = buildList {
        add(resources.getQuantityString(R.plurals.structure_blocks, blocks.size, blocks.size))
        if (sets > 1) add(resources.getQuantityString(R.plurals.structure_sets, sets, sets))
        if (includingSteps) add(resources.getQuantityString(R.plurals.structure_steps, stepCount, stepCount))
    }
    return parts.joinToString(WorkoutFormat.SEPARATOR)
}

@Composable
fun TimerDefinition.structureText(includingSteps: Boolean = false): String =
    structureText(LocalResources.current, includingSteps)

/** A duration shown as digits (`4:52`) but read out in words. */
@Composable
fun DurationText(
    seconds: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val spoken = spokenDuration(seconds)
    Text(
        text = WorkoutFormat.duration(seconds),
        modifier = modifier.semantics { contentDescription = spoken },
        color = color,
        style = style,
    )
}
