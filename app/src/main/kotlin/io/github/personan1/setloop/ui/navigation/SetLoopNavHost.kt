package io.github.personan1.setloop.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.personan1.setloop.ui.history.HistoryScreen
import io.github.personan1.setloop.ui.history.RecordDetailScreen
import io.github.personan1.setloop.ui.settings.SettingsScreen
import io.github.personan1.setloop.ui.settings.StepColorsScreen
import io.github.personan1.setloop.ui.timers.TimerEditorScreen
import io.github.personan1.setloop.ui.timers.TimerListScreen
import io.github.personan1.setloop.ui.timers.TimerPreviewScreen

/** Root scaffold: bottom navigation on the three tabs, hidden on the screens opened from them. */
@Composable
fun SetLoopNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navigator = remember(navController) { AppNavigator(navController) }
    val entry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelTab.entries.firstOrNull { it.isCurrent(entry?.destination) }

    Scaffold(
        modifier = modifier,
        // Screens apply the insets themselves: each has its own top bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) SetLoopNavigationBar(currentTab, navigator::openTab)
        },
    ) { padding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.TimerList,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<Route.TimerList> { TimerListScreen(navigator) }
            composable<Route.TimerPreview> { TimerPreviewScreen(it.toRoute<Route.TimerPreview>().timerId, navigator) }
            composable<Route.TimerEditor> { TimerEditorScreen(it.toRoute<Route.TimerEditor>().timerId, navigator) }
            composable<Route.History> { HistoryScreen(navigator) }
            composable<Route.RecordDetail> { RecordDetailScreen(it.toRoute<Route.RecordDetail>().recordId, navigator) }
            composable<Route.Settings> { SettingsScreen(navigator) }
            composable<Route.StepColors> { StepColorsScreen(navigator) }
        }
    }
}
