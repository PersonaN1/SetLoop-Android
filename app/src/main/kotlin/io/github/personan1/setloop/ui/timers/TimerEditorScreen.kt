package io.github.personan1.setloop.ui.timers

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.personan1.setloop.R
import io.github.personan1.setloop.ui.components.PlaceholderScreen
import io.github.personan1.setloop.ui.navigation.AppNavigator

/** [timerId] is null for a new timer. */
@Composable
fun TimerEditorScreen(timerId: String?, navigator: AppNavigator) {
    PlaceholderScreen(
        stringResource(if (timerId == null) R.string.default_name_new_timer else R.string.title_edit_timer),
        onBack = navigator::back,
    )
}
