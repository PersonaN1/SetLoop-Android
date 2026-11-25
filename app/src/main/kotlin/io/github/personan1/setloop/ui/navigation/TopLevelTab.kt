package io.github.personan1.setloop.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import io.github.personan1.setloop.R

/** Tabs of the bottom navigation bar, as in the iOS `RootView`. */
enum class TopLevelTab(val route: Route, @StringRes val label: Int, val icon: ImageVector) {
    Timers(Route.TimerList, R.string.tab_timers, Icons.Outlined.Timer),
    History(Route.History, R.string.tab_history, Icons.Outlined.History),
    Settings(Route.Settings, R.string.tab_settings, Icons.Outlined.Settings),
    ;

    fun isCurrent(destination: NavDestination?): Boolean = when (this) {
        Timers -> destination?.hasRoute<Route.TimerList>()
        History -> destination?.hasRoute<Route.History>()
        Settings -> destination?.hasRoute<Route.Settings>()
    } == true
}
