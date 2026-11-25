package io.github.personan1.setloop.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.StepRecord
import io.github.personan1.setloop.core.cappedName
import io.github.personan1.setloop.data.WorkoutRecordEntity
import io.github.personan1.setloop.ui.LocalAppContainer
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.DurationText
import io.github.personan1.setloop.ui.components.MetricRow
import io.github.personan1.setloop.ui.components.MinRowHeight
import io.github.personan1.setloop.ui.components.SetLoopCard
import io.github.personan1.setloop.ui.components.StatusBadge
import io.github.personan1.setloop.ui.components.StepLine
import io.github.personan1.setloop.ui.components.StepRecordStrip
import io.github.personan1.setloop.ui.components.StepTypeDot
import io.github.personan1.setloop.ui.components.TitledScreenScaffold
import io.github.personan1.setloop.ui.components.durationMetric
import io.github.personan1.setloop.ui.components.spokenDuration
import io.github.personan1.setloop.ui.components.stepsMetric
import io.github.personan1.setloop.ui.navigation.AppNavigator
import io.github.personan1.setloop.ui.theme.TimerTextStyles
import io.github.personan1.setloop.ui.workout.rememberWorkoutStarter
import io.github.personan1.setloop.workout.WorkoutFormat
import java.time.ZoneId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface RecordLoad {
    data object Loading : RecordLoad

    data object Missing : RecordLoad

    data class Loaded(val record: WorkoutRecordEntity) : RecordLoad
}

/** One saved workout: steps with outcomes, editable name and note, repeat and delete (SPEC D10). */
@Composable
fun RecordDetailScreen(recordId: String, navigator: AppNavigator) {
    val history = LocalAppContainer.current.history
    val load by produceState<RecordLoad>(RecordLoad.Loading, recordId) {
        value = history.get(recordId)?.let(RecordLoad::Loaded) ?: RecordLoad.Missing
    }
    when (val current = load) {
        RecordLoad.Loading -> TitledScreenScaffold(title = "", onBack = navigator::back) {}
        RecordLoad.Missing -> LaunchedEffect(Unit) { navigator.back() }
        is RecordLoad.Loaded -> RecordDetail(current.record, navigator)
    }
}

@Composable
private fun RecordDetail(record: WorkoutRecordEntity, navigator: AppNavigator) {
    val history = LocalAppContainer.current.history
    val scope = rememberCoroutineScope()
    val starter = rememberWorkoutStarter()
    var name by rememberSaveable(record.id) { mutableStateOf(record.name) }
    var note by rememberSaveable(record.id) { mutableStateOf(record.note) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val steps = remember(record.stepsData) { record.steps }

    // Every edit is written at once, so leaving the screen never loses it. A blank name is not stored.
    fun persist(newName: String, newNote: String) {
        scope.launch { withContext(NonCancellable) { history.updateText(record.id, newName, newNote) } }
    }

    TitledScreenScaffold(title = name.ifBlank { record.name }, onBack = navigator::back) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RecordSummaryCard(record, steps)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.cappedName
                        persist(name, note)
                    },
                    label = { Text(stringResource(R.string.record_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        persist(name, note)
                    },
                    label = { Text(stringResource(R.string.detail_note_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StepLog(steps)
            RecordActions(
                canRepeat = record.definition != null,
                onRepeat = { starter.startFromRecord(record.id) },
                onDelete = { confirmingDelete = true },
            )
        }
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_title),
            confirmText = stringResource(R.string.detail_delete_confirm),
            onConfirm = {
                confirmingDelete = false
                scope.launch {
                    withContext(NonCancellable) { history.deleteMany(listOf(record.id)) }
                }
                navigator.back()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
private fun RecordSummaryCard(record: WorkoutRecordEntity, steps: List<StepRecord>) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SetLoopCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    DurationText(record.duration, style = TimerTextStyles.total, color = MaterialTheme.colorScheme.onSurface)
                    StatusBadge(record.status)
                }
                MetricRow(
                    listOf(
                        durationMetric(stringResource(R.string.metric_work), record.workTime),
                        durationMetric(stringResource(R.string.metric_rest), record.restTime),
                        stepsMetric(record.completedSteps, record.totalSteps),
                    ),
                )
                StepRecordStrip(steps)
            }
        }
        Text(
            HistoryFormat.fullDateTime(record.startedAt, ZoneId.systemDefault(), locale),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun StepLog(steps: List<StepRecord>) {
    if (steps.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.detail_steps_header),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
        SetLoopCard {
            steps.forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                StepLogRow(step)
            }
        }
    }
}

@Composable
private fun StepLogRow(record: StepRecord) {
    val outcomeText = outcomeLabel(record.outcome)
    val spoken = stepSpokenTime(record)
    StepLine(
        name = record.step.name,
        modifier = Modifier
            .alpha(if (record.outcome == StepRecord.Outcome.Skipped) 0.5f else 1f)
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${record.step.name}, $spoken, $outcomeText" },
        mark = { StepTypeDot(record.step.type) },
        detail = {
            // The time actually spent; a step left early also shows its planned length.
            val shown = if (record.outcome == StepRecord.Outcome.Completed) {
                WorkoutFormat.duration(record.elapsed)
            } else {
                "${WorkoutFormat.duration(record.elapsed)} / ${WorkoutFormat.duration(record.step.duration)}"
            }
            Text(shown, style = TimerTextStyles.metric.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(
                imageVector = outcomeIcon(record.outcome),
                contentDescription = null,
                tint = outcomeTint(record.outcome),
                modifier = Modifier.padding(start = 8.dp).size(18.dp).clearAndSetSemantics {},
            )
        },
    )
}

@Composable
private fun outcomeLabel(outcome: StepRecord.Outcome): String = stringResource(
    when (outcome) {
        StepRecord.Outcome.Completed -> R.string.detail_outcome_completed
        StepRecord.Outcome.Skipped -> R.string.detail_outcome_skipped
        StepRecord.Outcome.Interrupted -> R.string.detail_outcome_interrupted
    },
)

@Composable
private fun stepSpokenTime(record: StepRecord): String {
    val elapsed = spokenDuration(record.elapsed)
    if (record.outcome == StepRecord.Outcome.Completed) return elapsed
    return stringResource(R.string.detail_step_spoken_partial, elapsed, spokenDuration(record.step.duration))
}

private fun outcomeIcon(outcome: StepRecord.Outcome): ImageVector = when (outcome) {
    StepRecord.Outcome.Completed -> Icons.Filled.Check
    StepRecord.Outcome.Skipped -> Icons.Filled.SkipNext
    StepRecord.Outcome.Interrupted -> Icons.Filled.Stop
}

@Composable
private fun outcomeTint(outcome: StepRecord.Outcome): Color = when (outcome) {
    StepRecord.Outcome.Completed -> MaterialTheme.colorScheme.tertiary
    StepRecord.Outcome.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant
    StepRecord.Outcome.Interrupted -> Color(0xFFE08A00)
}

@Composable
private fun RecordActions(canRepeat: Boolean, onRepeat: () -> Unit, onDelete: () -> Unit) {
    SetLoopCard {
        ActionRow(
            icon = Icons.Filled.Refresh,
            text = stringResource(R.string.detail_repeat),
            color = MaterialTheme.colorScheme.primary,
            enabled = canRepeat,
            onClick = onRepeat,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ActionRow(
            icon = Icons.Filled.Delete,
            text = stringResource(R.string.detail_delete),
            color = MaterialTheme.colorScheme.error,
            enabled = true,
            onClick = onDelete,
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
