package io.github.personan1.setloop.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.personan1.setloop.core.RGB
import io.github.personan1.setloop.core.StepColors
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.data.AppSettingsValues

/** Step type -> color from Settings. Provided by [SetLoopApp][io.github.personan1.setloop.ui.SetLoopApp]. */
val LocalStepColors = staticCompositionLocalOf { StepColors.DEFAULT }

/** The current settings snapshot. Provided next to [LocalStepColors]. */
val LocalAppSettings = staticCompositionLocalOf { AppSettingsValues() }

fun RGB.toColor(): Color = Color(argb)

fun StepColors.color(type: StepType): Color = this[type].toColor()

/** Color of this step type under the user's settings. */
val StepType.color: Color
    @Composable get() = LocalStepColors.current.color(this)

@Composable
fun ProvideAppSettings(settings: AppSettingsValues, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppSettings provides settings,
        LocalStepColors provides settings.stepColors,
        content = content,
    )
}
