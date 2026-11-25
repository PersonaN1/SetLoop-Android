package io.github.personan1.setloop.ui.settings

import androidx.annotation.StringRes
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.RGB
import io.github.personan1.setloop.core.StepColors
import io.github.personan1.setloop.core.StepType

enum class SwatchState { Selected, Available, Blocked }

/** A selected color always shows as selected, even when it would be refused now. */
fun StepColors.swatchState(type: StepType, color: RGB): SwatchState = when {
    this[type] == color -> SwatchState.Selected
    isAllowed(color, type) -> SwatchState.Available
    else -> SwatchState.Blocked
}

/** The mapping after picking [color] for [type]; unchanged when the pick is not allowed (DESIGN V8). */
fun StepColors.picked(type: StepType, color: RGB): StepColors =
    if (this[type] == color || !isAllowed(color, type)) this else copy(colors = colors + (type to color))

/** True when every type has its default color, even if the stored map is missing some types. */
val StepColors.isDefault: Boolean
    get() = StepType.entries.all { this[it] == StepColors.DEFAULT[it] }

@get:StringRes
val StepType.label: Int
    get() = when (this) {
        StepType.Prepare -> R.string.step_colors_type_prepare
        StepType.WorkA -> R.string.step_colors_type_work_a
        StepType.WorkB -> R.string.step_colors_type_work_b
        StepType.WorkC -> R.string.step_colors_type_work_c
        StepType.Rest -> R.string.step_colors_type_rest
        StepType.Cooldown -> R.string.step_colors_type_cooldown
    }

/** Color names of DESIGN V2, for the row value and screen readers. */
@StringRes
fun paletteName(color: RGB): Int = when (color.hex) {
    0xE8452C -> R.string.palette_red
    0xD63C7E -> R.string.palette_pink
    0x9C2BB0 -> R.string.palette_purple
    0x7B4FD6 -> R.string.palette_violet
    0x4B55C9 -> R.string.palette_indigo
    0x2F6BDB -> R.string.palette_blue
    0x0E7C9E -> R.string.palette_ocean
    0x0F8A8A -> R.string.palette_teal
    0x1E8E52 -> R.string.palette_green
    0x5F8F1A -> R.string.palette_olive
    0xB86200 -> R.string.palette_amber
    0x5B6470 -> R.string.palette_graphite
    else -> R.string.palette_custom
}
