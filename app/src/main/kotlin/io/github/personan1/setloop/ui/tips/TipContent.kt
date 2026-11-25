package io.github.personan1.setloop.ui.tips

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TouchApp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.tips.Tip

/** What a tip shows. Start and Merge have no icon: they are popups next to a button. */
internal class TipContent(
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
    val icon: ImageVector?,
)

internal val Tip.content: TipContent
    get() = when (this) {
        Tip.ListIntro -> TipContent(R.string.tip_list_intro_title, R.string.tip_list_intro_text, Icons.Default.TouchApp)
        Tip.CardActions -> TipContent(R.string.tip_card_actions_title, R.string.tip_card_actions_text, Icons.Default.PanTool)
        Tip.Start -> TipContent(R.string.tip_start_title, R.string.tip_start_text, null)
        Tip.Block -> TipContent(R.string.tip_block_title, R.string.tip_block_text, Icons.Default.Repeat)
        Tip.Merge -> TipContent(R.string.tip_merge_title, R.string.tip_merge_text, null)
        Tip.History -> TipContent(R.string.tip_history_title, R.string.tip_history_text, Icons.Default.History)
    }
