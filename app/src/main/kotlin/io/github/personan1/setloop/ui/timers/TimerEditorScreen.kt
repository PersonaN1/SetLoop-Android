package io.github.personan1.setloop.ui.timers

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.cappedName
import io.github.personan1.setloop.core.totalDuration
import io.github.personan1.setloop.data.DefaultNames
import io.github.personan1.setloop.ui.components.DurationText
import io.github.personan1.setloop.ui.components.GroupedRow
import io.github.personan1.setloop.ui.components.MinRowHeight
import io.github.personan1.setloop.ui.components.PreviewStripView
import io.github.personan1.setloop.ui.components.StepLine
import io.github.personan1.setloop.ui.components.StepTypeDot
import io.github.personan1.setloop.ui.components.SingleLineKeyboardOptions
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.hideKeyboardOnScroll
import io.github.personan1.setloop.ui.components.isKeyboardOpen
import io.github.personan1.setloop.ui.components.ReorderState
import io.github.personan1.setloop.ui.components.groupedShape
import io.github.personan1.setloop.ui.components.rememberReorderState
import io.github.personan1.setloop.ui.components.reorderHandle
import io.github.personan1.setloop.ui.components.reorderableItem
import io.github.personan1.setloop.ui.components.spokenDuration
import io.github.personan1.setloop.ui.navigation.AppNavigator
import java.util.UUID

/** The default step names in the language of [context]. */
fun stepNames(context: Context) = StepNames(
    prepare = context.getString(R.string.default_name_prepare),
    work = context.getString(R.string.default_name_work),
    rest = context.getString(R.string.default_name_rest),
    restBetweenSets = context.getString(R.string.default_name_rest_between_sets),
    cooldown = context.getString(R.string.default_name_cooldown),
)

/** [timerId] is null for a new timer. */
@Composable
fun TimerEditorScreen(timerId: String?, navigator: AppNavigator) {
    val appContext = LocalContext.current.applicationContext
    val viewModel = appViewModel(key = timerId ?: NEW_TIMER_KEY) {
        TimerEditorViewModel(timerId, it.timers, stepNames(appContext), DefaultNames.newTimerName(appContext))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.closeEvents.collect { navigator.back() } }

    val editor = state.editor
    if (editor == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    EditorContent(isNew = timerId == null, state = state, editor = editor, viewModel = viewModel, onClose = navigator::back)
}

private const val NEW_TIMER_KEY = "new-timer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorContent(
    isNew: Boolean,
    state: EditorUiState,
    editor: EditorState,
    viewModel: TimerEditorViewModel,
    onClose: () -> Unit,
) {
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }
    var nameFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val names = remember(context) { stepNames(context) }

    // Like Cancel: leaves selection mode first, then asks before dropping changes.
    val cancel = {
        when {
            editor.selecting -> viewModel.stopSelecting()
            editor.hasChanges -> confirmingDiscard = true
            else -> onClose()
        }
    }
    BackHandler(onBack = cancel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (isNew) R.string.default_name_new_timer else R.string.title_edit_timer),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (editor.hasChanges) confirmingDiscard = true else onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = editor.canSave && !state.saving) {
                        Text(stringResource(R.string.action_done), fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
        bottomBar = {
            // Over the keyboard the panel would cover half the screen.
            if (!nameFocused && !isKeyboardOpen) {
                BottomPanel(editor, onSelect = viewModel::startSelecting, onCancelSelect = viewModel::stopSelecting, onMerge = viewModel::merge)
            }
        },
    ) { padding ->
        EditorList(
            editor = editor,
            viewModel = viewModel,
            padding = padding,
            onNameFocus = { nameFocused = it },
        )
    }

    state.stepEdit?.let { edit ->
        StepSheet(
            edit = edit,
            names = names,
            onSave = viewModel::saveStep,
            onDelete = viewModel::deleteStep,
            onDismiss = viewModel::closeStepSheet,
        )
    }
    if (confirmingDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.timers_discard_title),
            confirmText = stringResource(R.string.timers_discard_confirm),
            onConfirm = {
                confirmingDiscard = false
                onClose()
            },
            onDismiss = { confirmingDiscard = false },
        )
    }
    state.mergeError?.let { MergeErrorDialog(it, onDismiss = viewModel::dismissMergeError) }
    if (state.limitReached) TimerLimitDialog(onDismiss = viewModel::dismissLimit)
}

@Composable
private fun MergeErrorDialog(error: TimerDefinition.MergeError, onDismiss: () -> Unit) {
    val (title, message) = when (error) {
        TimerDefinition.MergeError.NotContiguous ->
            stringResource(R.string.timers_merge_error_title) to stringResource(R.string.timers_merge_error_not_contiguous)
        TimerDefinition.MergeError.WholeBlock ->
            stringResource(R.string.timers_merge_error_title) to stringResource(R.string.timers_merge_error_whole_block)
        TimerDefinition.MergeError.TooManyBlocks ->
            stringResource(R.string.timers_merge_error_too_many_title) to
                stringResource(R.string.timers_merge_error_too_many, Limits.maxBlocks)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.timers_action_ok)) } },
    )
}

@Composable
private fun EditorList(
    editor: EditorState,
    viewModel: TimerEditorViewModel,
    padding: PaddingValues,
    onNameFocus: (Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(
        listState = listState,
        onMove = { from, to -> viewModel.moveStep(from as UUID, to as UUID) },
        onDragFinished = {},
        canDrop = { from, to ->
            val block = (from as? UUID)?.let(editor::blockOf)
            block != null && block == (to as? UUID)?.let(editor::blockOf)
        },
    )
    val definition = editor.definition

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().hideKeyboardOnScroll(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "name") { NameField(editor.name, viewModel::setName, onNameFocus) }

        sectionHeader("header-general", R.string.timers_section_general)
        editorRows(editor.generalRows(), editor, viewModel, reorder)

        definition.blocks.forEachIndexed { index, block ->
            item(key = "header-${block.id}") {
                BlockHeader(
                    number = index + 1,
                    canDuplicate = editor.canAddBlock,
                    onDuplicate = { viewModel.duplicateBlock(block.id) },
                    onDelete = { viewModel.deleteBlock(block.id) },
                )
            }
            editorRows(blockRows(block), editor, viewModel, reorder)
            if (block.steps.size >= Limits.maxStepsPerBlock) {
                footer("footer-steps-${block.id}") { stringResource(R.string.timers_limit_steps, Limits.maxStepsPerBlock) }
            }
        }

        item(key = "space-add-block") { Spacer(Modifier.size(24.dp)) }
        editorRows(listOf(EditorRow.AddBlock), editor, viewModel, reorder)
        if (!editor.canAddBlock) footer("footer-blocks") { stringResource(R.string.timers_limit_blocks, Limits.maxBlocks) }

        sectionHeader("header-finish", R.string.timers_section_finish)
        editorRows(editor.finishRows(), editor, viewModel, reorder)
        item(key = "footer-finish") { FinishFooter(editor.validationError == TimerDefinition.ValidationError.EmptyPlan) }
    }
}

private fun LazyListScope.sectionHeader(key: String, titleRes: Int) {
    item(key = key) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )
    }
}

private fun LazyListScope.footer(key: String, text: @Composable () -> String) {
    item(key = key) {
        Text(
            text(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
    }
}

/** A group of rows that reads as one card. */
private fun LazyListScope.editorRows(
    rows: List<EditorRow>,
    editor: EditorState,
    viewModel: TimerEditorViewModel,
    reorder: ReorderState,
) {
    itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
        EditorRowItem(row, index == 0, index == rows.lastIndex, editor, viewModel, reorder)
    }
}

@Composable
private fun LazyItemScope.EditorRowItem(
    row: EditorRow,
    first: Boolean,
    last: Boolean,
    editor: EditorState,
    viewModel: TimerEditorViewModel,
    reorder: ReorderState,
) {
    val selecting = editor.selecting
    GroupedRow(first = first, last = last, modifier = reorderableItem(reorder, row.key)) {
        when (row) {
            is EditorRow.Optional -> OptionalStepRow(
                row = row,
                enabled = !selecting,
                onOpen = { viewModel.openStep(row.slot, row.step) },
            )
            EditorRow.Sets -> StepperRow(
                label = stringResource(R.string.timers_sets),
                value = editor.definition.sets,
                onChange = viewModel::setSets,
            )
            is EditorRow.Cycles -> StepperRow(
                label = stringResource(R.string.timers_cycles),
                value = row.block.repeats,
                onChange = { viewModel.setRepeats(row.block.id, it) },
            )
            is EditorRow.BlockStep -> {
                val steps = editor.definition.blocks.firstOrNull { it.id == row.blockId }?.steps.orEmpty()
                val index = steps.indexOfFirst { it.id == row.step.id }
                BlockStepRow(
                    modifier = Modifier,
                    step = row.step,
                    selecting = selecting,
                    selected = row.step.id in editor.selection,
                    onOpen = { viewModel.openStep(StepSlot.InBlock(row.blockId, row.step.id), row.step) },
                    onToggle = { viewModel.toggleSelected(row.step.id) },
                    handleModifier = Modifier.reorderHandle(reorder, row.key),
                    onMoveUp = if (index > 0) ({ viewModel.moveStepBy(row.blockId, index, -1) }) else null,
                    onMoveDown = if (index in 0 until steps.lastIndex) ({ viewModel.moveStepBy(row.blockId, index, 1) }) else null,
                )
            }
            is EditorRow.AddStep -> AddRow(
                text = stringResource(R.string.timers_add_step),
                enabled = !selecting && editor.canAddStep(row.blockId),
                onClick = { viewModel.openStep(StepSlot.InBlock(row.blockId, null)) },
            )
            EditorRow.AddBlock -> AddRow(
                text = stringResource(R.string.timers_add_block),
                enabled = !selecting && editor.canAddBlock,
                onClick = viewModel::addBlock,
            )
            EditorRow.SkipLastRest -> SwitchRow(
                label = stringResource(R.string.timers_skip_last_rest),
                checked = editor.definition.skipLastRest,
                onChange = viewModel::setSkipLastRest,
            )
        }
    }
}

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit, onFocusChange: (Boolean) -> Unit) {
    val state = rememberTextFieldState(name)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val nameDescription = stringResource(R.string.timers_name_label)
    val capName = remember {
        InputTransformation {
            val text = asCharSequence().toString()
            val capped = text.cappedName
            if (capped != text) replace(0, length, capped)
        }
    }
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.collect(onNameChange) }

    Surface(shape = groupedShape(first = true, last = true), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        TextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
                .semantics { contentDescription = nameDescription },
            placeholder = { Text(stringResource(R.string.default_name_new_timer)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = capName,
            keyboardOptions = SingleLineKeyboardOptions,
            onKeyboardAction = KeyboardActionHandler {
                keyboard?.hide()
                focusManager.clearFocus()
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun BlockHeader(number: Int, canDuplicate: Boolean, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 24.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.timers_block_number, number),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.timers_block_options, number))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.timers_duplicate_block)) },
                    enabled = canDuplicate,
                    onClick = { open = false; onDuplicate() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.timers_delete_block), color = MaterialTheme.colorScheme.error) },
                    onClick = { open = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun OptionalStepRow(row: EditorRow.Optional, enabled: Boolean, onOpen: () -> Unit) {
    val step = row.step
    if (step == null) {
        val title = when (row.slot) {
            StepSlot.Prepare -> R.string.timers_add_prepare
            StepSlot.RestBetweenSets -> R.string.timers_add_rest_between_sets
            else -> R.string.timers_add_cooldown
        }
        AddRow(stringResource(title), enabled, onOpen)
    } else {
        StepRowContent(step, enabled = enabled, onOpen = onOpen)
    }
}

/** An existing step outside the blocks. Not selectable: only block steps take part in merging. */
@Composable
private fun StepRowContent(step: Step, enabled: Boolean, onOpen: () -> Unit) {
    val spoken = spokenDuration(step.duration)
    val modifier = if (enabled) Modifier.clickable(onClick = onOpen) else Modifier
    StepLine(
        name = step.name,
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = "${step.name}, $spoken" }
            .padding(horizontal = 16.dp),
        mark = { StepTypeDot(step.type) },
        detail = { StepDuration(step.duration) },
    )
}

@Composable
private fun StepDuration(seconds: Int) {
    DurationText(
        seconds = seconds,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun BlockStepRow(
    modifier: Modifier,
    step: Step,
    selecting: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    handleModifier: Modifier,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    val spoken = spokenDuration(step.duration)
    val moveUp = stringResource(R.string.timers_move_up)
    val moveDown = stringResource(R.string.timers_move_down)
    val handleDescription = stringResource(R.string.timers_reorder_handle, step.name)
    val interaction = if (selecting) {
        modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
    } else {
        modifier.clickable(onClick = onOpen)
    }
    StepLine(
        name = step.name,
        modifier = interaction
            .semantics(mergeDescendants = true) {
                contentDescription = "${step.name}, $spoken"
                if (selecting) {
                    customActions = buildList {
                        onMoveUp?.let { add(CustomAccessibilityAction(moveUp) { it(); true }) }
                        onMoveDown?.let { add(CustomAccessibilityAction(moveDown) { it(); true }) }
                    }
                }
            }
            .padding(start = 16.dp, end = if (selecting) 4.dp else 16.dp),
        mark = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selecting) Checkbox(checked = selected, onCheckedChange = null)
                StepTypeDot(step.type)
            }
        },
        detail = {
            StepDuration(step.duration)
            if (selecting) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = handleDescription,
                    modifier = Modifier.size(48.dp).then(handleModifier).padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun StepperRow(label: String, value: Int, onChange: (Int) -> Unit) {
    val decrease = stringResource(R.string.timers_decrease, label)
    val increase = stringResource(R.string.timers_increase, label)
    val times = stringResource(R.string.timers_times, value)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MinRowHeight).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$label, $value" },
        )
        Text(
            times,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
        IconButton(onClick = { onChange(value - 1) }, enabled = value > Limits.repeats.first) {
            Icon(Icons.Default.Remove, contentDescription = decrease)
        }
        IconButton(onClick = { onChange(value + 1) }, enabled = value < Limits.repeats.last) {
            Icon(Icons.Default.Add, contentDescription = increase)
        }
    }
}

@Composable
private fun AddRow(text: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight + 4.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight + 4.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun FinishFooter(showNothingToRun: Boolean) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.timers_skip_last_rest_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The reason Done is disabled, which is otherwise hard to guess.
        if (showNothingToRun) {
            Text(
                stringResource(R.string.timers_nothing_to_run),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Pinned total, strip and step selection actions (DESIGN V11). On a short screen they share one row. */
@Composable
private fun BottomPanel(editor: EditorState, onSelect: () -> Unit, onCancelSelect: () -> Unit, onMerge: () -> Unit) {
    val cleaned = editor.cleaned
    val total = cleaned.totalDuration.toInt()
    val heightDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val compact = heightDp < COMPACT_HEIGHT
    val totalRow: @Composable (Modifier) -> Unit = { modifier ->
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.timers_total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DurationText(seconds = total, style = MaterialTheme.typography.titleSmall)
        }
    }
    val actions: @Composable () -> Unit = {
        if (editor.selecting) {
            TextButton(onClick = onCancelSelect) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = onMerge, enabled = editor.selection.isNotEmpty()) {
                Text(stringResource(R.string.timers_merge_into_block))
            }
        } else {
            TextButton(onClick = onSelect) { Text(stringResource(R.string.timers_select_steps)) }
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                totalRow(Modifier)
                PreviewStripView(cleaned, modifier = Modifier.weight(1f), height = 8.dp)
                actions()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    totalRow(Modifier.weight(1f))
                }
                PreviewStripView(cleaned, height = 8.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = MinRowHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    content = { actions() },
                )
            }
        }
    }
}

private val COMPACT_HEIGHT = 480.dp
