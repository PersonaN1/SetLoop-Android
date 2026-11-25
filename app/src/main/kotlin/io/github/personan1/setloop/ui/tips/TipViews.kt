package io.github.personan1.setloop.ui.tips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.tips.Tip
import io.github.personan1.setloop.ui.components.SetLoopCard

/** Tip in the feed: accent icon, title, text and a close button (DESIGN V12). */
@Composable
fun TipCard(tip: Tip, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val content = tip.content
    SetLoopCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content.icon?.let { icon ->
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp).size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(content.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(content.text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.tip_close),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Rich tooltip with an arrow above [anchor], open while [tip] is non-null. Tapping outside it closes the
 * tip for good, as a popover tip on iOS. Long press on the anchor shows nothing: the tip is not a hint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipPopup(tip: Tip?, onDismiss: () -> Unit, anchor: @Composable () -> Unit) {
    val state = rememberTooltipState(isPersistent = true)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(tip) {
        if (tip == null) {
            state.dismiss()
        } else {
            // Returns when the tooltip is closed from outside; a change of `tip` cancels it instead.
            state.show()
            currentOnDismiss()
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            val content = (tip ?: return@TooltipBox).content
            RichTooltip(
                title = { Text(stringResource(content.title)) },
                caretShape = TooltipDefaults.caretShape(),
            ) {
                Text(stringResource(content.text))
            }
        },
        state = state,
        enableUserInput = false,
        content = anchor,
    )
}
