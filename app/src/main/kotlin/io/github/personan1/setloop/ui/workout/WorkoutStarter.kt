package io.github.personan1.setloop.ui.workout

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.personan1.setloop.R
import io.github.personan1.setloop.data.DefaultNames
import io.github.personan1.setloop.ui.LocalAppContainer
import io.github.personan1.setloop.workout.AndroidStartEnvironment
import io.github.personan1.setloop.workout.StartProgress
import io.github.personan1.setloop.workout.WorkoutPreparer
import io.github.personan1.setloop.workout.WorkoutService
import io.github.personan1.setloop.workout.WorkoutStartFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Starts workouts from a screen. Get it with [rememberWorkoutStarter]. */
@Stable
class WorkoutStarter internal constructor(
    private val flow: WorkoutStartFlow,
    private val scope: CoroutineScope,
) {
    /** Loads the timer and starts the workout (asking for permissions first when needed). */
    fun start(timerId: String) {
        scope.launch { flow.begin(timerId) }
    }

    /** Starts the workout saved in a history record again, from its stored definition snapshot. */
    fun startFromRecord(recordId: String) {
        scope.launch { flow.beginFromRecord(recordId) }
    }

    internal fun onNotificationPermissionResult() {
        scope.launch { flow.onNotificationPermissionResult() }
    }

    internal fun confirmBackgroundWarning() {
        scope.launch { flow.confirmBackgroundWarning() }
    }

    internal fun cancel() = flow.cancel()

    internal val progress get() = flow.progress
}

/**
 * Holder for the start flow: `val starter = rememberWorkoutStarter()`, then `starter.start(timerId)` from a
 * click handler. It also shows the battery-restriction dialog, so call it once per screen that starts workouts.
 */
@SuppressLint("InlinedApi")
@Composable
fun rememberWorkoutStarter(): WorkoutStarter {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var savedProgress by rememberSaveable { mutableStateOf<StartProgress?>(null) }
    // The launcher is created before the flow, which needs it to ask; the flow is set by the time a result arrives.
    var starter by remember { mutableStateOf<WorkoutStarter?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        starter?.onNotificationPermissionResult()
    }

    val current = remember(container) {
        val appContext = context.applicationContext
        val preparer = WorkoutPreparer(container.timers, container.settings) { DefaultNames.from(appContext) }
        val flow = WorkoutStartFlow(
            environment = AndroidStartEnvironment(appContext),
            prepare = preparer::prepare,
            launch = { WorkoutService.start(appContext, it.definition, it.timerName, it.options) },
            requestNotificationPermission = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            prepareFromRecord = { recordId ->
                container.history.get(recordId)?.let { record ->
                    record.definition?.let { preparer.prepare(record.name, it) }
                }
            },
            initial = savedProgress,
        )
        WorkoutStarter(flow, scope).also { starter = it }
    }

    val progress by current.progress.collectAsStateWithLifecycle()
    LaunchedEffect(progress) { savedProgress = progress }
    if (progress?.stage == StartProgress.Stage.BackgroundWarning) {
        BackgroundRestrictedDialog(
            onStartAnyway = current::confirmBackgroundWarning,
            onOpenSettings = {
                openAppSettings(context)
                current.cancel()
            },
            onDismiss = current::cancel,
        )
    }
    return current
}

@Composable
private fun BackgroundRestrictedDialog(onStartAnyway: () -> Unit, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.background_restricted_title)) },
        text = { Text(stringResource(R.string.background_restricted_message)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.background_restricted_open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onStartAnyway) { Text(stringResource(R.string.background_restricted_start_anyway)) }
        },
    )
}

/** The system page of the app, where Battery > Unrestricted lifts the background restriction. */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
