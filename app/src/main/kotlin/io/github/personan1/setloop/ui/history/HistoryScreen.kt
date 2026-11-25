package io.github.personan1.setloop.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.data.HistoryDay
import io.github.personan1.setloop.data.WeekMetrics
import io.github.personan1.setloop.data.WorkoutRecordEntity
import io.github.personan1.setloop.ui.LocalAppContainer
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.DurationText
import io.github.personan1.setloop.ui.components.MetricData
import io.github.personan1.setloop.ui.components.MetricRow
import io.github.personan1.setloop.ui.components.MinRowHeight
import io.github.personan1.setloop.ui.components.SelectionBottomBar
import io.github.personan1.setloop.ui.components.SetLoopCard
import io.github.personan1.setloop.ui.components.StepRecordStrip
import io.github.personan1.setloop.ui.components.TitledScreenScaffold
import io.github.personan1.setloop.ui.navigation.AppNavigator
import io.github.personan1.setloop.ui.navigation.HideBottomBarWhile
import io.github.personan1.setloop.workout.WorkoutFormat
import java.time.ZoneId
import kotlinx.coroutines.launch

/** Saved workouts by day with this week's totals (SPEC D10, DESIGN V6). */
@Composable
fun HistoryScreen(navigator: AppNavigator) {
    val history = LocalAppContainer.current.history
    val days by remember(history) { history.observeDays() }.collectAsStateWithLifecycle(initialValue = null)
    val week by remember(history) { history.observeWeekMetrics() }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var selection by rememberSaveable(stateSaver = HistorySelection.Saver) { mutableStateOf(HistorySelection()) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    val loadedDays = days
    val allIds = remember(loadedDays) { loadedDays.orEmpty().flatMap { day -> day.records.map { it.id } } }
    // Records deleted elsewhere (or all of them here) leave selection mode as on iOS.
    LaunchedEffect(allIds) { if (loadedDays != null) selection = selection.retain(allIds) }
    HideBottomBarWhile(selection.isSelecting)
    BackHandler(enabled = selection.isSelecting) { selection = selection.finish() }

    TitledScreenScaffold(
        title = stringResource(R.string.tab_history),
        largeTitle = true,
        actions = {
            TextButton(
                onClick = { selection = if (selection.isSelecting) selection.finish() else selection.start() },
                enabled = allIds.isNotEmpty() || selection.isSelecting,
            ) {
                Text(stringResource(if (selection.isSelecting) R.string.action_done else R.string.history_select))
            }
        },
        bottomBar = {
            if (selection.isSelecting) {
                SelectionBottomBar(
                    selectedCount = selection.count,
                    allSelected = selection.allSelected(allIds),
                    onToggleAll = { selection = selection.toggleAll(allIds) },
                    onDelete = { confirmingDelete = true },
                )
            }
        },
    ) { padding ->
        when {
            loadedDays == null -> Box(Modifier.fillMaxSize().padding(padding))
            loadedDays.isEmpty() -> EmptyHistory(Modifier.padding(padding))
            else -> HistoryList(
                days = loadedDays,
                week = week,
                selection = selection,
                onOpen = { id -> if (selection.isSelecting) selection = selection.toggle(id) else navigator.openRecord(id) },
                onLongPress = { id -> selection = if (selection.isSelecting) selection.toggle(id) else selection.start(id) },
                // A swipe deletes at once, without a confirmation (SPEC D10).
                onDelete = { id -> scope.launch { history.deleteMany(listOf(id)) } },
                padding = padding,
            )
        }
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = pluralStringResource(R.plurals.history_delete_title, selection.count, selection.count),
            confirmText = stringResource(R.string.history_delete_confirm),
            onConfirm = {
                confirmingDelete = false
                val ids = selection.selected
                scope.launch { history.deleteMany(ids) }
                selection = selection.copy(selected = emptySet())
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.history_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryList(
    days: List<HistoryDay>,
    week: WeekMetrics?,
    selection: HistorySelection,
    onOpen: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onDelete: (String) -> Unit,
    padding: PaddingValues,
) {
    val locale = LocalConfiguration.current.locales[0]
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (week != null) item(key = "week") { WeekCard(week) }
        items(days, key = { it.day.toEpochDay() }) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    HistoryFormat.dayHeader(day.day, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                )
                SetLoopCard {
                    day.records.forEachIndexed { index, record ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwipeToDeleteRow(enabled = !selection.isSelecting, onDelete = { onDelete(record.id) }) {
                            HistoryRow(
                                record = record,
                                selecting = selection.isSelecting,
                                selected = record.id in selection.selected,
                                onClick = { onOpen(record.id) },
                                onLongClick = { onLongPress(record.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteRow(enabled: Boolean, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val delete = stringResource(R.string.detail_delete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            (target == SwipeToDismissBoxValue.EndToStart).also { if (it) onDelete() }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = enabled,
        modifier = Modifier.semantics { customActions = listOf(CustomAccessibilityAction(delete) { onDelete(); true }) },
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
            }
        },
        content = { Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, content = content) },
    )
}

@Composable
private fun WeekCard(week: WeekMetrics) {
    val resources = LocalResources.current
    SetLoopCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_this_week),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MetricRow(
                listOf(
                    MetricData(stringResource(R.string.history_metric_workouts), week.count.toString()),
                    MetricData(
                        label = stringResource(R.string.history_metric_time),
                        value = HistoryFormat.totalTime(resources, week.totalDuration),
                    ),
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    record: WorkoutRecordEntity,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val steps = remember(record.stepsData) { record.steps }
    val time = HistoryFormat.time(record.startedAt, ZoneId.systemDefault(), locale)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight)
            .combinedClickable(role = if (selecting) Role.Checkbox else Role.Button, onClick = onClick, onLongClick = onLongClick)
            .semantics { if (selecting) this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) Checkbox(checked = selected, onCheckedChange = null)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    record.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                DurationText(record.duration, style = MaterialTheme.typography.bodyLarge)
            }
            val partial = if (record.status == WorkoutStatus.Partial) stringResource(R.string.history_partial) else null
            Text(
                listOfNotNull(time, partial).joinToString(WorkoutFormat.SEPARATOR),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StepRecordStrip(steps, height = 6.dp)
        }
    }
}
