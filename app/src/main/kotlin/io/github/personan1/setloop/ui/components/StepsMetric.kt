package io.github.personan1.setloop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.personan1.setloop.R

/** "Steps 7/9", read out as "7 of 9". */
@Composable
fun stepsMetric(completed: Int, total: Int) = MetricData(
    label = stringResource(R.string.record_metric_steps),
    value = stringResource(R.string.record_steps_value, completed, total),
    spokenValue = stringResource(R.string.record_steps_spoken, completed, total),
)
