package io.github.personan1.setloop.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Lets a screen take the root bottom navigation bar away while its own bottom bar is up (selection mode,
 * DESIGN V6). Screens only talk to this holder, never to each other.
 */
@Stable
class BottomBarVisibility {
    private var requests by mutableIntStateOf(0)

    val hidden: Boolean get() = requests > 0

    internal fun hide() {
        requests++
    }

    internal fun release() {
        requests--
    }
}

val LocalBottomBarVisibility = compositionLocalOf { BottomBarVisibility() }

/** Hides the root navigation bar for as long as [hidden] is true and this call is in the composition. */
@Composable
fun HideBottomBarWhile(hidden: Boolean) {
    val visibility = LocalBottomBarVisibility.current
    DisposableEffect(visibility, hidden) {
        if (hidden) visibility.hide()
        onDispose { if (hidden) visibility.release() }
    }
}
