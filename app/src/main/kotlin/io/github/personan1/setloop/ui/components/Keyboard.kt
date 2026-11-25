package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

/** Single-line field keyboard (DESIGN V7): the Return key reads "Done" and closes the keyboard. */
val SingleLineKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

@Composable
fun singleLineKeyboardActions(): KeyboardActions {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return KeyboardActions(
        onDone = {
            keyboard?.hide()
            focusManager.clearFocus()
        },
    )
}

/** Dragging the scrolling content closes the keyboard (DESIGN V7). */
@Composable
fun Modifier.hideKeyboardOnScroll(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return nestedScroll(
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available != Offset.Zero) hideKeyboard(focusManager, keyboard)
                return Offset.Zero
            }
        },
    )
}

private fun hideKeyboard(focusManager: FocusManager, keyboard: SoftwareKeyboardController?) {
    keyboard?.hide()
    focusManager.clearFocus()
}

/** True while the on-screen keyboard is open, to hide pinned bars that would cover half the screen. */
val isKeyboardOpen: Boolean
    @Composable get() = WindowInsets.ime.getBottom(LocalDensity.current) > 0
