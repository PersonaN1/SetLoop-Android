package io.github.personan1.setloop.ui.timers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.duration
import io.github.personan1.setloop.core.stepCount
import io.github.personan1.setloop.tips.Tip
import io.github.personan1.setloop.core.totalDuration
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.MetricRow
import io.github.personan1.setloop.ui.components.MinRowHeight
import io.github.personan1.setloop.ui.components.PreviewStripView
import io.github.personan1.setloop.ui.components.SetLoopCard
import io.github.personan1.setloop.ui.components.StepLine
import io.github.personan1.setloop.ui.components.WithMaxFontScale
import io.github.personan1.setloop.ui.components.spokenDuration
import io.github.personan1.setloop.ui.components.structureText
import io.github.personan1.setloop.ui.components.workRestOtherMetrics
import io.github.personan1.setloop.ui.navigation.AppNavigator
import io.github.personan1.setloop.ui.theme.TimerTextStyles
import io.github.personan1.setloop.ui.tips.TipPopup
import io.github.personan1.setloop.ui.tips.rememberTips
import io.github.personan1.setloop.ui.workout.rememberWorkoutStarter
import io.github.personan1.setloop.workout.WorkoutFormat

@Composable
fun TimerPreviewScreen(timerId: String, navigator: AppNavigator) {
    val viewModel = appViewModel(key = timerId) { TimerPreviewViewModel(timerId, it.timers) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val starter = rememberWorkoutStarter()
    val tips = rememberTips()
    // Back on the list, the card actions tip may show.
    LaunchedEffect(Unit) { tips.openedTimer() }

    when (val current = state) {
        TimerPreviewState.Loading -> Box(Modifier.fillMaxSize())
        TimerPreviewState.Missing -> {
            Box(Modifier.fillMaxSize())
            LaunchedEffect(Unit) { navigator.back() }
        }
        is TimerPreviewState.Loaded -> PreviewContent(
            timer = current.timer,
            limitReached = current.limitReached,
            onBack = navigator::back,
            onEdit = { navigator.editTimer(timerId) },
            onDuplicate = viewModel::duplicate,
            onDelete = viewModel::delete,
            onDismissLimit = viewModel::dismissLimit,
            startTip = tips.visible(Tip.Start),
            onDismissStartTip = { tips.dismiss(Tip.Start) },
            onStart = {
                tips.dismiss(Tip.Start)
                starter.start(timerId)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewContent(
    timer: TimerItem,
    limitReached: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDismissLimit: () -> Unit,
    startTip: Tip?,
    onDismissStartTip: () -> Unit,
    onStart: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val definition = timer.definition

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(timer.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.timers_action_edit))
                    }
                    OverflowMenu(onEdit = onEdit, onDuplicate = onDuplicate, onDelete = { confirmingDelete = true })
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { StartBar(startTip, onDismissStartTip, onStart) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SummaryCard(definition) }
            item {
                Text(
                    stringResource(R.string.timers_structure),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                )
            }
            item { StructureCard(definition.structureRows()) }
        }
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.timers_delete_title),
            text = stringResource(R.string.timers_delete_message),
            confirmText = stringResource(R.string.timers_action_delete),
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
    if (limitReached) TimerLimitDialog(onDismiss = onDismissLimit)
}

@Composable
private fun OverflowMenu(onEdit: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.timers_action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.timers_action_edit)) }, onClick = { open = false; onEdit() })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.timers_action_duplicate)) },
                onClick = { open = false; onDuplicate() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.timers_action_delete), color = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

@Composable
private fun StartBar(tip: Tip?, onDismissTip: () -> Unit, onStart: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        TipPopup(tip, onDismissTip) { StartButton(onStart) }
    }
}

@Composable
private fun StartButton(onStart: () -> Unit) {
    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
            .heightIn(min = 52.dp),
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            stringResource(R.string.action_start),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SummaryCard(definition: TimerDefinition) {
    val total = definition.totalDuration.toInt()
    val totalDescription = stringResource(R.string.timers_total_description, spokenDuration(total))
    SetLoopCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                definition.structureText(includingSteps = true),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WithMaxFontScale(maxScale = 1.3f) {
                Text(
                    WorkoutFormat.duration(total),
                    style = TimerTextStyles.total,
                    modifier = Modifier.semantics { contentDescription = totalDescription },
                )
            }
            PreviewStripView(definition, height = 12.dp)
            MetricRow(
                workRestOtherMetrics(
                    workSeconds = definition.duration(StepType.Category.Work).toInt(),
                    restSeconds = definition.duration(StepType.Category.Rest).toInt(),
                    otherSeconds = definition.duration(StepType.Category.Other).toInt(),
                ),
            )
        }
    }
}

@Composable
private fun StructureCard(rows: List<StructureRow>) {
    SetLoopCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            rows.forEachIndexed { index, row ->
                when (row) {
                    is StructureRow.StepRow -> StepLine(
                        name = row.step.name,
                        type = row.step.type,
                        seconds = row.step.duration,
                        modifier = Modifier.padding(start = if (row.indented) 24.dp else 0.dp),
                    )
                    is StructureRow.SetsRow -> HeaderRow(
                        title = stringResource(R.string.timers_sets),
                        detail = row.rest?.let {
                            stringResource(R.string.timers_times_with_rest, row.count, WorkoutFormat.duration(it.duration))
                        } ?: stringResource(R.string.timers_times, row.count),
                    )
                    is StructureRow.BlockRow -> HeaderRow(
                        title = stringResource(R.string.timers_block_number, row.number),
                        detail = if (row.repeats > 1) stringResource(R.string.timers_times, row.repeats) else null,
                    )
                }
                if (index < rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun HeaderRow(title: String, detail: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MinRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
