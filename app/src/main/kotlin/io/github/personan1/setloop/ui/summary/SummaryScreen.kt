package io.github.personan1.setloop.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.core.WorkoutSummary
import io.github.personan1.setloop.tips.Tip
import io.github.personan1.setloop.ui.LocalAppContainer
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.DurationText
import io.github.personan1.setloop.ui.components.MetricRow
import io.github.personan1.setloop.ui.components.StatusBadge
import io.github.personan1.setloop.ui.components.StepRecordStrip
import io.github.personan1.setloop.ui.components.accentColor
import io.github.personan1.setloop.ui.components.durationMetric
import io.github.personan1.setloop.ui.components.stepsMetric
import io.github.personan1.setloop.ui.theme.TimerTextStyles
import io.github.personan1.setloop.ui.tips.TipCard
import io.github.personan1.setloop.ui.tips.rememberTips
import io.github.personan1.setloop.workout.WorkoutResult
import kotlinx.coroutines.launch

/** Shown full screen over the app while `WorkoutController.state` is `Finished` (SPEC D3, DESIGN V5). */
@Composable
fun SummaryScreen(result: WorkoutResult, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val history = LocalAppContainer.current.history
    val scope = rememberCoroutineScope()
    val tips = rememberTips()
    val saveFlow = remember(result, history, onDismiss) {
        SummarySaveFlow(
            persist = { name, note ->
                history.save(
                    name = name,
                    note = note,
                    startedAt = result.startedAtEpochMillis,
                    summary = result.summary,
                    definition = result.definition,
                    defaultName = result.timerName,
                )
            },
            dismiss = onDismiss,
        )
    }
    val isSaved by saveFlow.isSaved.collectAsState()
    var fields by rememberSaveable(result, stateSaver = SummaryFieldsSaver) { mutableStateOf(SummaryFields(result.timerName)) }
    var confirmingDiscard by rememberSaveable(result) { mutableStateOf(false) }
    var recordCount by rememberSaveable(result) { mutableStateOf(0) }
    LaunchedEffect(history) { recordCount = history.count() }

    val edited = fields.isEdited(result.timerName)
    BackHandler { if (edited) confirmingDiscard = true else saveFlow.discard() }

    SummaryContent(
        summary = result.summary,
        fields = fields,
        onFieldsChange = { fields = it },
        isSaved = isSaved,
        historyFull = recordCount >= Limits.maxHistoryRecords,
        tip = tips.visible(Tip.History),
        onCloseTip = tips::dismiss,
        onSave = {
            tips.dismiss(Tip.History)
            scope.launch { saveFlow.save(fields) }
        },
        onDiscard = { if (edited) confirmingDiscard = true else saveFlow.discard() },
        modifier = modifier,
    )

    if (confirmingDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.summary_discard_title),
            text = stringResource(R.string.summary_discard_message),
            confirmText = stringResource(R.string.summary_discard_confirm),
            onConfirm = {
                confirmingDiscard = false
                saveFlow.discard()
            },
            onDismiss = { confirmingDiscard = false },
        )
    }
}

@Composable
private fun SummaryContent(
    summary: WorkoutSummary,
    fields: SummaryFields,
    onFieldsChange: (SummaryFields) -> Unit,
    isSaved: Boolean,
    historyFull: Boolean,
    tip: Tip?,
    onCloseTip: (Tip) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SummaryHeader(summary)
                    SummaryMetrics(summary)
                    StepRecordStrip(summary.steps)
                    SummaryFieldsForm(fields, onFieldsChange, enabled = !isSaved)
                    if (tip != null) TipCard(tip, onClose = { onCloseTip(tip) })
                    if (historyFull) {
                        Text(
                            stringResource(R.string.summary_history_full, Limits.maxHistoryRecords),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SummaryButtons(isSaved = isSaved, onSave = onSave, onDiscard = onDiscard)
        }
    }
}

@Composable
private fun SummaryHeader(summary: WorkoutSummary) {
    val accent = summary.status.accentColor()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The badge below says the same.
        Icon(
            imageVector = if (summary.status == WorkoutStatus.Complete) Icons.Filled.CheckCircle else Icons.Filled.Flag,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(56.dp),
        )
        DurationText(summary.duration, style = TimerTextStyles.total, color = MaterialTheme.colorScheme.onBackground)
        StatusBadge(summary.status)
    }
}

@Composable
private fun SummaryMetrics(summary: WorkoutSummary) {
    MetricRow(
        listOf(
            durationMetric(stringResource(R.string.metric_work), summary.time(StepType.Category.Work)),
            durationMetric(stringResource(R.string.metric_rest), summary.time(StepType.Category.Rest)),
            stepsMetric(summary.completedSteps, summary.totalSteps),
        ),
    )
}

@Composable
private fun SummaryFieldsForm(fields: SummaryFields, onChange: (SummaryFields) -> Unit, enabled: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = fields.name,
            onValueChange = { onChange(fields.withName(it)) },
            label = { Text(stringResource(R.string.record_name_label)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = fields.note,
            onValueChange = { onChange(fields.copy(note = it)) },
            label = { Text(stringResource(R.string.summary_note_label)) },
            minLines = 2,
            maxLines = 5,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryButtons(isSaved: Boolean, onSave: () -> Unit, onDiscard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = onSave,
            enabled = !isSaved,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.summary_save), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
        TextButton(onClick = onDiscard, enabled = !isSaved, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.summary_dont_save), color = MaterialTheme.colorScheme.error)
        }
    }
}
