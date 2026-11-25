package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.personan1.setloop.R

/**
 * Screen with a top bar and a [content] that scrolls under it. [largeTitle] gives the collapsing large title of
 * the tabs (DESIGN V7); without it the title is the small one of a screen opened from a tab. [onBack] adds the up arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitledScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = if (largeTitle) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }
    val titleContent: @Composable () -> Unit = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        }
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
    )
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (largeTitle) {
                LargeTopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = colors.containerColor,
                        scrolledContainerColor = colors.scrolledContainerColor,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = colors,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = bottomBar,
        content = content,
    )
}
