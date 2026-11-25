package io.github.personan1.setloop.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Palette
import io.github.personan1.setloop.core.RGB
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepColors
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.ui.LocalAppContainer
import io.github.personan1.setloop.ui.components.ConfirmDialog
import io.github.personan1.setloop.ui.components.MinRowHeight
import io.github.personan1.setloop.ui.components.PreviewStripView
import io.github.personan1.setloop.ui.components.StepTypeDot
import io.github.personan1.setloop.ui.components.TitledScreenScaffold
import io.github.personan1.setloop.ui.navigation.AppNavigator
import io.github.personan1.setloop.ui.theme.LocalAppSettings
import io.github.personan1.setloop.ui.theme.toColor
import kotlinx.coroutines.launch

/** One step of every type, so each color shows in the preview strip. */
private val PreviewDefinition = TimerDefinition(
    prepare = Step(name = "", duration = 10, type = StepType.Prepare),
    blocks = listOf(
        Block(
            steps = listOf(
                Step(name = "", duration = 20, type = StepType.WorkA),
                Step(name = "", duration = 10, type = StepType.Rest),
                Step(name = "", duration = 20, type = StepType.WorkB),
                Step(name = "", duration = 10, type = StepType.Rest),
                Step(name = "", duration = 20, type = StepType.WorkC),
            ),
        ),
    ),
    cooldown = Step(name = "", duration = 10, type = StepType.Cooldown),
)

/** Settings -> Step Colors: the palette opens inside the row of a type; below are a preview and a reset (DESIGN V8). */
@Composable
fun StepColorsScreen(navigator: AppNavigator) {
    val colors = LocalAppSettings.current.stepColors
    val store = LocalAppContainer.current.settings
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf<StepType?>(null) }
    var confirmingReset by rememberSaveable { mutableStateOf(false) }

    TitledScreenScaffold(title = stringResource(R.string.title_step_colors), onBack = navigator::back) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SettingsGroup(title = null, footer = stringResource(R.string.step_colors_footer)) {
                    StepType.entries.forEachIndexed { index, type ->
                        if (index > 0) RowDivider()
                        StepTypeColorRow(
                            type = type,
                            colors = colors,
                            expanded = expanded == type,
                            onToggle = { expanded = if (expanded == type) null else type },
                            onPick = { color -> scope.launch { store.setStepColors(colors.picked(type, color)) } },
                        )
                    }
                }
                SettingsGroup(title = stringResource(R.string.step_colors_preview)) {
                    PreviewStripView(PreviewDefinition, modifier = Modifier.padding(16.dp), height = 12.dp)
                }
                SettingsGroup(title = null) {
                    TextButton(
                        onClick = { confirmingReset = true },
                        enabled = !colors.isDefault,
                        modifier = Modifier.fillMaxWidth().heightIn(min = MinRowHeight),
                    ) { Text(stringResource(R.string.step_colors_reset)) }
                }
            }
        }
    }

    if (confirmingReset) {
        ConfirmDialog(
            title = stringResource(R.string.step_colors_reset_title),
            confirmText = stringResource(R.string.step_colors_reset_confirm),
            onConfirm = {
                confirmingReset = false
                scope.launch { store.setStepColors(StepColors.DEFAULT) }
            },
            onDismiss = { confirmingReset = false },
            destructive = false,
        )
    }
}

@Composable
private fun StepTypeColorRow(
    type: StepType,
    colors: StepColors,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (RGB) -> Unit,
) {
    val current = colors[type]
    val colorName = stringResource(paletteName(current))
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinRowHeight)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = colorName }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepTypeDot(current.toColor())
            Text(stringResource(type.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(colorName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 0f else -90f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            PaletteGrid(type, colors, onPick)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteGrid(type: StepType, colors: StepColors, onPick: (RGB) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        maxItemsInEachRow = SWATCHES_PER_ROW,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Palette.colors.forEach { color -> Swatch(color, colors.swatchState(type, color), onPick) }
    }
}

@Composable
private fun Swatch(color: RGB, state: SwatchState, onPick: (RGB) -> Unit) {
    val name = stringResource(paletteName(color))
    val unavailable = stringResource(R.string.step_colors_unavailable, name)
    val enabled = state != SwatchState.Blocked
    Box(
        modifier = Modifier
            .size(SWATCH_TOUCH_SIZE)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.RadioButton) { onPick(color) }
            .semantics {
                selected = state == SwatchState.Selected
                contentDescription = if (enabled) name else unavailable
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SWATCH_SIZE)
                .background(color.toColor().copy(alpha = if (enabled) 1f else BLOCKED_ALPHA), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                SwatchState.Selected -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                SwatchState.Blocked -> Icon(Icons.Filled.Block, contentDescription = null, tint = color.toColor(), modifier = Modifier.size(20.dp))
                SwatchState.Available -> Unit
            }
        }
    }
}

private const val SWATCHES_PER_ROW = 6
private const val BLOCKED_ALPHA = 0.2f
private val SWATCH_TOUCH_SIZE = 48.dp
private val SWATCH_SIZE = 36.dp
