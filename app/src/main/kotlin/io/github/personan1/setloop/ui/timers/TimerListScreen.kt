package io.github.personan1.setloop.ui.timers

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.tips.Tip
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.GroupedRow
import io.github.personan1.setloop.ui.components.PreviewStripView
import io.github.personan1.setloop.ui.components.SelectionBottomBar
import io.github.personan1.setloop.ui.components.isLargeFontScale
import io.github.personan1.setloop.ui.components.pressTapGestures
import io.github.personan1.setloop.ui.components.reorderHandle
import io.github.personan1.setloop.ui.components.reorderableItem
import io.github.personan1.setloop.ui.components.rememberReorderState
import io.github.personan1.setloop.ui.components.spokenDuration
import io.github.personan1.setloop.ui.components.structureText
import io.github.personan1.setloop.ui.navigation.AppNavigator
import io.github.personan1.setloop.ui.navigation.HideBottomBarWhile
import io.github.personan1.setloop.ui.tips.TipCard
import io.github.personan1.setloop.ui.tips.rememberTips
import io.github.personan1.setloop.ui.workout.rememberWorkoutStarter
import io.github.personan1.setloop.workout.WorkoutFormat

private val PlayButtonSize = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerListScreen(navigator: AppNavigator) {
    val viewModel = appViewModel { TimerListViewModel(it.timers) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val starter = rememberWorkoutStarter()
    val tips = rememberTips()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val selection = state.selection
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmingSelection by rememberSaveable { mutableStateOf(false) }
    val timers = state.timers
    HideBottomBarWhile(selection.active)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tab_timers)) },
                actions = {
                    if (selection.active) {
                        TextButton(onClick = viewModel::finishSelecting) { Text(stringResource(R.string.action_done)) }
                    } else {
                        TextButton(onClick = viewModel::startSelecting, enabled = !timers.isNullOrEmpty()) {
                            Text(stringResource(R.string.timers_action_select))
                        }
                        IconButton(onClick = { if (viewModel.canAddTimer()) navigator.newTimer() }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.timers_action_new))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (selection.active) {
                val ids = timers.orEmpty().map { it.id }
                SelectionBottomBar(
                    selectedCount = selection.selected.size,
                    allSelected = selection.allSelected(ids),
                    onToggleAll = viewModel::toggleAll,
                    onDelete = { confirmingSelection = true },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                timers == null -> Unit
                timers.isEmpty() -> EmptyState(onNew = { if (viewModel.canAddTimer()) navigator.newTimer() })
                else -> TimerCards(
                    state = state,
                    viewModel = viewModel,
                    // Tips are out of the way in selection mode (DESIGN V12).
                    tip = if (selection.active) null else tips.visible(Tip.ListIntro, Tip.CardActions),
                    onCloseTip = tips::dismiss,
                    onOpen = navigator::openTimer,
                    onEdit = {
                        tips.dismiss(Tip.CardActions)
                        navigator.editTimer(it)
                    },
                    onDuplicate = {
                        tips.dismiss(Tip.CardActions)
                        viewModel.duplicate(it)
                    },
                    onStart = starter::start,
                    onRequestDelete = {
                        tips.dismiss(Tip.CardActions)
                        pendingDelete = it
                    },
                )
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.timers_delete_title),
            text = stringResource(R.string.timers_delete_message),
            confirmText = stringResource(R.string.timers_action_delete),
            onConfirm = {
                viewModel.delete(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
    if (confirmingSelection) {
        ConfirmDialog(
            title = pluralStringResource(R.plurals.timers_delete_selected_title, selection.selected.size, selection.selected.size),
            text = stringResource(R.string.timers_delete_message),
            confirmText = stringResource(R.string.timers_action_delete),
            onConfirm = {
                viewModel.deleteSelected()
                confirmingSelection = false
            },
            onDismiss = { confirmingSelection = false },
        )
    }
    if (state.limitReached) TimerLimitDialog(onDismiss = viewModel::dismissLimit)
}

/** Shown only when adding or duplicating is refused; the list has no counter (SPEC D9). */
@Composable
fun TimerLimitDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timers_limit_title)) },
        text = { Text(stringResource(R.string.timers_limit_message, Limits.maxTimers)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.timers_action_ok)) } },
    )
}

@Composable
private fun EmptyState(onNew: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.timers_empty_title), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onNew) { Text(stringResource(R.string.timers_action_new)) }
    }
}

@Composable
private fun TimerCards(
    state: TimerListUiState,
    viewModel: TimerListViewModel,
    tip: Tip?,
    onCloseTip: (Tip) -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onStart: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
) {
    val timers = state.timers.orEmpty()
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(
        listState = listState,
        onMove = { from, to -> viewModel.move(from as String, to as String) },
        onDragFinished = viewModel::saveOrder,
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        if (tip != null) {
            item(key = "tip") { TipCard(tip, onClose = { onCloseTip(tip) }, modifier = Modifier.padding(bottom = 16.dp)) }
        }
        itemsIndexed(timers, key = { _, item -> item.id }) { index, item ->
            val last = index == timers.lastIndex
            TimerCard(
                item = item,
                first = index == 0,
                last = last,
                selecting = state.selection.active,
                selected = item.id in state.selection.selected,
                modifier = reorderableItem(reorder, item.id),
                handleModifier = Modifier.reorderHandle(reorder, item.id),
                onOpen = { onOpen(item.id) },
                onToggle = { viewModel.toggleSelected(item.id) },
                onStart = { onStart(item.id) },
                onEdit = { onEdit(item.id) },
                onDuplicate = { onDuplicate(item.id) },
                onDelete = { onRequestDelete(item.id) },
                onMoveUp = if (index > 0) ({ viewModel.moveBy(item.id, -1) }) else null,
                onMoveDown = if (!last) ({ viewModel.moveBy(item.id, 1) }) else null,
            )
        }
    }
}

@Composable
private fun TimerCard(
    item: TimerItem,
    modifier: Modifier,
    first: Boolean,
    last: Boolean,
    selecting: Boolean,
    selected: Boolean,
    handleModifier: Modifier,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    var menuOpen by rememberSaveable(item.id) { mutableStateOf(false) }
    val structure = item.definition.structureText()
    val meta = "${WorkoutFormat.duration(item.totalDuration)}${WorkoutFormat.SEPARATOR}$structure"
    val spokenMeta = "${spokenDuration(item.totalDuration)}${WorkoutFormat.SEPARATOR}$structure"
    val edit = stringResource(R.string.timers_action_edit)
    val duplicate = stringResource(R.string.timers_action_duplicate)
    val delete = stringResource(R.string.timers_action_delete)
    val moveUp = stringResource(R.string.timers_move_up)
    val moveDown = stringResource(R.string.timers_move_down)

    val interactionSource = remember { MutableInteractionSource() }
    val tapModifier = if (selecting) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
    } else {
        // Not `clickable`: it would merge the Start button into the card for screen readers.
        Modifier.pressTapGestures(interactionSource, onTap = onOpen, onLongPress = { menuOpen = true })
    }
    GroupedRow(first = first, last = last, modifier = modifier) {
        Box {
            Column(
                modifier = tapModifier
                    .indication(interactionSource, ripple())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CardHeader(
                    modifier = Modifier,
                    name = item.name,
                    meta = meta,
                    spokenMeta = spokenMeta,
                    textModifier = Modifier.semantics(mergeDescendants = true) {
                        if (!selecting) {
                            onClick { onOpen(); true }
                            onLongClick(label = edit) { menuOpen = true; true }
                        }
                        customActions = buildList {
                            if (selecting) {
                                onMoveUp?.let { add(CustomAccessibilityAction(moveUp) { it(); true }) }
                                onMoveDown?.let { add(CustomAccessibilityAction(moveDown) { it(); true }) }
                            } else {
                                add(CustomAccessibilityAction(edit) { onEdit(); true })
                                add(CustomAccessibilityAction(duplicate) { onDuplicate(); true })
                                add(CustomAccessibilityAction(delete) { onDelete(); true })
                            }
                        }
                    },
                    leading = if (selecting) ({ Checkbox(checked = selected, onCheckedChange = null) }) else null,
                    trailing = {
                        if (selecting) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = stringResource(R.string.timers_reorder_handle, item.name),
                                modifier = Modifier.size(PlayButtonSize).then(handleModifier).padding(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            PlayButton(name = item.name, onClick = onStart)
                        }
                    },
                )
                PreviewStripView(item.definition)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(edit) }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text(duplicate) }, onClick = { menuOpen = false; onDuplicate() })
                DropdownMenuItem(
                    text = { Text(delete, color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

/** Name and meta with the controls beside them; at large font scale the controls go under the text. */
@Composable
private fun CardHeader(
    modifier: Modifier,
    name: String,
    meta: String,
    spokenMeta: String,
    textModifier: Modifier,
    leading: (@Composable () -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    val texts: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier.then(textModifier), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = spokenMeta },
            )
        }
    }
    if (isLargeFontScale) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                leading?.invoke()
                texts(Modifier.weight(1f))
            }
            trailing()
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            texts(Modifier.weight(1f))
            trailing()
        }
    }
}

/** Only the button starts a workout; the rest of the card opens the preview (SPEC D9). */
@Composable
private fun PlayButton(name: String, onClick: () -> Unit) {
    val description = stringResource(R.string.timers_start_named, name)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(PlayButtonSize)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .semantics { contentDescription = description },
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}
