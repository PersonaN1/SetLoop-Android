package io.github.personan1.setloop.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.workout.WorkoutResult

/** Shown full screen over the app while `WorkoutController.state` is `Finished`. Placeholder until the summary is built. */
@Composable
fun SummaryScreen(result: WorkoutResult, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onDismiss)
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(result.timerName, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.title_summary), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
    }
}
