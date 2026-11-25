package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.PreviewStrip
import io.github.personan1.setloop.core.StepRecord
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.ui.theme.LocalStepColors
import io.github.personan1.setloop.ui.theme.color

/** Opacity of skipped steps in a strip of passed steps (DESIGN V5). */
const val SKIPPED_STEP_ALPHA = 0.25f

/** One colored piece of a strip: its duration is the width weight. */
data class StripPiece(val type: StepType, val duration: Int, val alpha: Float = 1f)

/**
 * Colored structure strip of a timer with the detail level picked by DESIGN V4. Groups with repeats
 * get an "xN" caption; several groups (levels 3-4) share the width equally. Hidden from accessibility.
 */
@Composable
fun PreviewStripView(definition: TimerDefinition, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val strip = remember(definition) { PreviewStrip.from(definition) }
    val labeled = strip.groups.any { it.repeats > 1 }
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        strip.groups.forEach { group ->
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (labeled) {
                    Text(
                        text = if (group.repeats > 1) stringResource(R.string.repeat_count_compact, group.repeats) else " ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                StepStrip(group.segments.map { StripPiece(it.type, it.duration) }, height = height)
            }
        }
    }
}

/** Strip of passed steps: completed bright, skipped at [SKIPPED_STEP_ALPHA] (DESIGN V5, V6). */
@Composable
fun StepRecordStrip(steps: List<StepRecord>, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val pieces = remember(steps) {
        steps.map {
            StripPiece(
                type = it.step.type,
                duration = it.step.duration,
                alpha = if (it.outcome == StepRecord.Outcome.Skipped) SKIPPED_STEP_ALPHA else 1f,
            )
        }
    }
    StepStrip(pieces, modifier.clearAndSetSemantics {}, height)
}

/** Draws [pieces] as one capsule of rounded segments, colored by the user's step colors. */
@Composable
fun StepStrip(pieces: List<StripPiece>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val stepColors = LocalStepColors.current
    val colors = pieces.map { stepColors.color(it.type).copy(alpha = it.alpha) }
    Canvas(modifier = modifier.fillMaxWidth().height(height).clip(CircleShape)) {
        drawStrip(pieces.map { it.duration }, colors)
    }
}

private fun DrawScope.drawStrip(durations: List<Int>, colors: List<Color>) {
    val rects = layoutStrip(durations, size.width, density)
    rects.forEachIndexed { index, rect ->
        val radius = minOf(STRIP_SEGMENT_RADIUS_DP * density, rect.width / 2f)
        drawRoundRect(
            color = colors[index],
            topLeft = Offset(rect.left, 0f),
            size = Size(rect.width, size.height),
            cornerRadius = CornerRadius(radius),
        )
    }
}
