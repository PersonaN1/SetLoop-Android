package io.github.personan1.setloop.ui.timers

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.MinRowHeight
import io.github.personan1.setloop.ui.components.SingleLineKeyboardOptions
import io.github.personan1.setloop.ui.components.StepTypeDot
import io.github.personan1.setloop.ui.components.WheelPicker
import io.github.personan1.setloop.ui.components.singleLineKeyboardActions
import java.util.UUID

@StringRes
private fun StepType.titleRes(): Int = when (this) {
    StepType.Prepare -> R.string.timers_type_prepare
    StepType.WorkA -> R.string.timers_type_work_a
    StepType.WorkB -> R.string.timers_type_work_b
    StepType.WorkC -> R.string.timers_type_work_c
    StepType.Rest -> R.string.timers_type_rest
    StepType.Cooldown -> R.string.timers_type_cooldown
}

private val StepDraftSaver = listSaver<StepDraft, Any>(
    save = { listOf(it.step.id.toString(), it.step.name, it.step.duration, it.step.type.rawValue) },
    restore = {
        StepDraft(
            Step(
                id = UUID.fromString(it[0] as String),
                name = it[1] as String,
                duration = it[2] as Int,
                type = requireNotNull(StepType.fromRawValue(it[3] as String)),
            ),
        )
    },
)

/**
 * Step sheet (DESIGN V11): name, type list (not for prepare, cooldown and rest between sets) and the
 * minutes and seconds wheels. A new step offers Cancel, an existing one Delete with confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepSheet(
    edit: StepEdit,
    names: StepNames,
    onSave: (Step) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable(edit.step.id, stateSaver = StepDraftSaver) { mutableStateOf(StepDraft(edit.step)) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val keyboardActions = singleLineKeyboardActions()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SheetHeader(
                isNew = edit.isNew,
                canSave = draft.canSave,
                onCancel = onDismiss,
                onDelete = { confirmingDelete = true },
                onDone = { onSave(draft.finished(names)) },
            )
            OutlinedTextField(
                value = draft.step.name,
                onValueChange = { draft = draft.withName(it) },
                label = { Text(stringResource(R.string.timers_name_label)) },
                singleLine = true,
                keyboardOptions = SingleLineKeyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!edit.slot.hasFixedType) {
                SectionTitle(stringResource(R.string.timers_step_type))
                StepType.entries.forEach { type ->
                    TypeRow(type, selected = type == draft.step.type, onClick = { draft = draft.withType(type, names) })
                }
            }
            SectionTitle(stringResource(R.string.timers_step_duration))
            DurationWheels(draft, onChange = { draft = it })
        }
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.timers_step_delete_title),
            confirmText = stringResource(R.string.timers_action_delete),
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
private fun SheetHeader(isNew: Boolean, canSave: Boolean, onCancel: () -> Unit, onDelete: () -> Unit, onDone: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (isNew) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        } else {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.timers_action_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        Text(
            stringResource(R.string.timers_step_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDone, enabled = canSave) {
            Text(stringResource(R.string.action_done), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TypeRow(type: StepType, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepTypeDot(type)
        Text(stringResource(type.titleRes()), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DurationWheels(draft: StepDraft, onChange: (StepDraft) -> Unit) {
    val minutesLabel = stringResource(R.string.timers_minutes_label)
    val secondsLabel = stringResource(R.string.timers_seconds_label)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        WheelPicker(
            count = StepDraft.MAX_MINUTES + 1,
            selectedIndex = draft.minutes,
            onSelected = { onChange(draft.withMinutes(it)) },
            label = { it.toString() },
            description = minutesLabel,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.timers_minutes_unit), modifier = Modifier.padding(horizontal = 8.dp))
        WheelPicker(
            count = StepDraft.MAX_SECONDS + 1,
            selectedIndex = draft.seconds,
            onSelected = { onChange(draft.withSeconds(it)) },
            label = { "%02d".format(it) },
            description = secondsLabel,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.timers_seconds_unit), modifier = Modifier.padding(horizontal = 8.dp))
    }
}
