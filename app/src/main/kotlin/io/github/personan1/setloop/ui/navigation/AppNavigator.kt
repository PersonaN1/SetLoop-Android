package io.github.personan1.setloop.ui.navigation

import androidx.compose.runtime.Stable
import androidx.navigation.NavController

/**
 * What screens may do to navigation. Screens take an [AppNavigator] instead of a `NavController`, so the
 * graph in `SetLoopNavHost` does not change when a screen's content does.
 */
@Stable
class AppNavigator(private val navController: NavController) {
    fun back() {
        navController.popBackStack()
    }

    fun openTimer(timerId: String) = navController.navigate(Route.TimerPreview(timerId))

    fun editTimer(timerId: String) = navController.navigate(Route.TimerEditor(timerId))

    fun newTimer() = navController.navigate(Route.TimerEditor())

    fun openRecord(recordId: String) = navController.navigate(Route.RecordDetail(recordId))

    fun openStepColors() = navController.navigate(Route.StepColors)

    /** Opens a bottom navigation tab, keeping the back stack of each tab. */
    fun openTab(tab: TopLevelTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
