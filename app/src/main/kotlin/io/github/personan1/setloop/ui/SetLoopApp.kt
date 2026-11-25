package io.github.personan1.setloop.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.personan1.setloop.data.AppContainer
import io.github.personan1.setloop.ui.navigation.SetLoopNavHost
import io.github.personan1.setloop.ui.summary.SummaryScreen
import io.github.personan1.setloop.ui.theme.ProvideAppSettings
import io.github.personan1.setloop.ui.theme.SetLoopTheme
import io.github.personan1.setloop.ui.theme.isDarkTheme
import io.github.personan1.setloop.ui.workout.WorkoutScreen
import io.github.personan1.setloop.workout.WorkoutController
import io.github.personan1.setloop.workout.WorkoutUiState

/** Repositories and settings for screens: `LocalAppContainer.current.timers`. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("LocalAppContainer is not provided") }

/**
 * The whole UI: theme from settings, the navigation graph, and the workout over everything while
 * `WorkoutController.state` is Active (workout screen) or Finished (summary screen).
 */
@Composable
fun SetLoopApp(container: AppContainer) {
    val settings by container.settings.values.collectAsStateWithLifecycle(initialValue = null)
    val loaded = settings ?: return
    val workout by WorkoutController.instance.state.collectAsStateWithLifecycle()
    val controller = WorkoutController.instance

    CompositionLocalProvider(LocalAppContainer provides container) {
        ProvideAppSettings(loaded) {
            SetLoopTheme(loaded.theme) {
                val workoutShown = workout !is WorkoutUiState.Idle
                SystemBars(lightIcons = isDarkTheme(loaded.theme) || workout is WorkoutUiState.Active)
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Hidden from screen readers while the workout covers it, but kept so its back stack survives.
                        Box(modifier = Modifier.fillMaxSize().semantics { if (workoutShown) hideFromAccessibility() }) {
                            SetLoopNavHost()
                        }
                        when (val current = workout) {
                            WorkoutUiState.Idle -> Unit
                            is WorkoutUiState.Active -> WorkoutScreen(
                                state = current,
                                onTogglePause = controller::togglePause,
                                onSkip = controller::skip,
                                onBack = controller::back,
                                onFinish = controller::finish,
                                modifier = Modifier.pointerInput(Unit) {},
                            )
                            is WorkoutUiState.Finished -> Surface(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
                                SummaryScreen(current.result, onDismiss = controller::dismissResult)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Edge-to-edge with transparent bars; [lightIcons] is for dark or saturated backgrounds. */
@Composable
private fun SystemBars(lightIcons: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, lightIcons) {
        val style = if (lightIcons) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose {}
    }
}
