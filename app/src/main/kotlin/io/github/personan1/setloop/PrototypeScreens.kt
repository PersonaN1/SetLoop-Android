package io.github.personan1.setloop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.personan1.setloop.core.Presets
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.workout.ElapsedRealtimeClock
import io.github.personan1.setloop.workout.WorkoutFormat
import io.github.personan1.setloop.workout.WorkoutResult
import io.github.personan1.setloop.workout.WorkoutTexts
import io.github.personan1.setloop.workout.WorkoutUiState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** Prototype screens: just enough to drive the workout service. Not the final design. */
@Composable
fun PresetList(presets: List<Presets.Preset>, onStart: (Int) -> Unit) {
    Column(modifier = Modifier.safeDrawingPadding().padding(16.dp)) {
        Text(stringResource(R.string.prototype_title), style = MaterialTheme.typography.headlineMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
            itemsIndexed(presets) { index, preset ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { onStart(index) }) { Text(stringResource(R.string.action_start)) }
                }
            }
        }
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState.Active,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    var now by remember { mutableStateOf(ElapsedRealtimeClock.now()) }
    LaunchedEffect(state) {
        while (!state.isPaused) {
            now = ElapsedRealtimeClock.now()
            delay(100.milliseconds)
        }
        now = ElapsedRealtimeClock.now()
    }
    val texts = WorkoutTexts(LocalContext.current)
    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(state.color.argb))
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(state.step.step.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(WorkoutFormat.time(state.remaining(now)), color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold)
        Text(texts.position(state.step).ifEmpty { " " }, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(state.next?.let(texts::next).orEmpty().ifEmpty { " " }, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, colors = buttonColors) { Text(stringResource(R.string.action_back)) }
            OutlinedButton(onClick = onTogglePause, colors = buttonColors) {
                Text(stringResource(if (state.isPaused) R.string.action_resume else R.string.action_pause))
            }
            OutlinedButton(onClick = onSkip, colors = buttonColors) { Text(stringResource(R.string.action_skip)) }
        }
        OutlinedButton(onClick = onFinish, colors = buttonColors, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.action_finish))
        }
    }
}

@Composable
fun ResultScreen(result: WorkoutResult, onClose: () -> Unit) {
    val summary = result.summary
    val title = if (summary.status == WorkoutStatus.Complete) R.string.result_complete else R.string.result_partial
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(result.timerName, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.result_duration, WorkoutFormat.time(summary.duration.seconds)))
        Text(stringResource(R.string.result_steps, summary.completedSteps, summary.totalSteps))
        Text(stringResource(R.string.result_work_time, WorkoutFormat.time(summary.time(StepType.Category.Work))))
        Text(stringResource(R.string.result_rest_time, WorkoutFormat.time(summary.time(StepType.Category.Rest))))
        Button(onClick = onClose) { Text(stringResource(R.string.action_close)) }
    }
}
