package io.github.personan1.setloop.ui.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.personan1.setloop.tips.Tip
import io.github.personan1.setloop.tips.TipsState
import io.github.personan1.setloop.tips.TipsStore
import io.github.personan1.setloop.ui.LocalAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The tips of one screen: ask [visible] what to show, report events with the other functions. */
@Stable
class Tips internal constructor(
    private val state: TipsState?,
    private val store: TipsStore,
    private val scope: CoroutineScope,
) {
    /** The tip to show now, or null. Nothing shows until the stored state is read, so no tip flashes. */
    fun visible(vararg candidates: Tip): Tip? = state?.visible(*candidates)

    /** The close button or the action the tip speaks about. */
    fun dismiss(tip: Tip) {
        scope.launch { store.dismiss(tip) }
    }

    fun openedTimer() {
        scope.launch { store.openedTimer() }
    }

    fun editorLeft() {
        scope.launch { store.editorLeft() }
    }

    fun showAgain() {
        scope.launch { store.showAgain() }
    }
}

@Composable
fun rememberTips(): Tips {
    val store = LocalAppContainer.current.tips
    val scope = rememberCoroutineScope()
    val state by remember(store) { store.state }.collectAsStateWithLifecycle(initialValue = null)
    return Tips(state, store, scope)
}
