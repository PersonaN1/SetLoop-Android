package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Tap and long press with press feedback on [interactionSource], without the semantics of `clickable`.
 * For a card that holds its own buttons: `clickable` would merge them into the card for screen readers,
 * so the card exposes its actions through its own semantics instead.
 */
@Composable
fun Modifier.pressTapGestures(
    interactionSource: MutableInteractionSource,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
): Modifier {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    return pointerInput(interactionSource) {
        detectTapGestures(
            onPress = { offset ->
                val press = PressInteraction.Press(offset)
                interactionSource.emit(press)
                if (tryAwaitRelease()) {
                    interactionSource.emit(PressInteraction.Release(press))
                } else {
                    interactionSource.emit(PressInteraction.Cancel(press))
                }
            },
            onTap = { currentOnTap() },
            onLongPress = { currentOnLongPress() },
        )
    }
}
