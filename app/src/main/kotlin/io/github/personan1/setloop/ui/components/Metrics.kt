package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.ui.theme.TimerTextStyles
import io.github.personan1.setloop.workout.WorkoutFormat

/** A value with a caption under it. [spokenValue] is what a screen reader says instead of [value]. */
@Immutable
data class MetricData(val label: String, val value: String, val spokenValue: String = value)

/** A length of time as a metric: digits on screen, words for a screen reader. */
@Composable
fun durationMetric(label: String, seconds: Int) =
    MetricData(label, WorkoutFormat.duration(seconds), spokenDuration(seconds))

@Composable
fun MetricTile(metric: MetricData, modifier: Modifier = Modifier) {
    val description = "${metric.label}, ${metric.spokenValue}"
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(metric.value, style = TimerTextStyles.metric, textAlign = TextAlign.Center)
        Text(
            metric.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Metrics side by side; stacked at large font scale, where several columns break the digits. */
@Composable
fun MetricRow(metrics: List<MetricData>, modifier: Modifier = Modifier) {
    if (isLargeFontScale) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            metrics.forEach { MetricTile(it) }
        }
    } else {
        Row(modifier = modifier.fillMaxWidth()) {
            metrics.forEach { MetricTile(it, Modifier.weight(1f)) }
        }
    }
}

/** The Work / Rest / Other metrics of a timer or a workout, in this order (DESIGN V9). */
@Composable
fun workRestOtherMetrics(workSeconds: Int, restSeconds: Int, otherSeconds: Int): List<MetricData> = listOf(
    durationMetric(stringResource(R.string.metric_work), workSeconds),
    durationMetric(stringResource(R.string.metric_rest), restSeconds),
    durationMetric(stringResource(R.string.metric_other), otherSeconds),
)
