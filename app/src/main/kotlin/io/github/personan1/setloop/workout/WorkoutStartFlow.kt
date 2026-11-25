package io.github.personan1.setloop.workout

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.data.AppSettings
import io.github.personan1.setloop.data.AppSettingsValues
import io.github.personan1.setloop.data.DefaultNames
import io.github.personan1.setloop.data.TimerRepository
import java.io.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** Everything [WorkoutService.start] needs, resolved from a saved timer and the current settings. */
data class PreparedWorkout(val timerName: String, val definition: TimerDefinition, val options: WorkoutOptions)

fun AppSettingsValues.toWorkoutOptions(): WorkoutOptions {
    val defaults = WorkoutOptions()
    return WorkoutOptions(
        soundSet = soundSet ?: defaults.soundSet,
        soundsEnabled = soundSet != null,
        voicePrompts = voicePrompts,
        vibration = vibration,
        ignoreSilentMode = ignoreSilentMode,
        showOnLockScreen = showOnLockScreen,
        pauseOnInterruption = pauseOnInterruption,
        stepColors = stepColors,
    )
}

/** Loads a saved timer, renames default-named steps to the interface language and reads the settings. */
class WorkoutPreparer(
    private val timers: TimerRepository,
    private val settings: AppSettings,
    private val defaultNames: () -> DefaultNames,
) {
    /** Null when the timer no longer exists. */
    suspend fun prepare(timerId: String): PreparedWorkout? {
        val timer = timers.get(timerId) ?: return null
        val names = defaultNames()
        return PreparedWorkout(
            timerName = names.localize(timer.name),
            definition = names.localize(timer.definition),
            options = settings.values.first().toWorkoutOptions(),
        )
    }
}

/** What the start flow asks about the device. A fake in tests. */
interface StartEnvironment {
    /** Android 13+, permission not granted and not asked before. */
    fun needsNotificationPermission(): Boolean

    /** Called right before the system dialog is shown, so the permission is requested only once. */
    fun markNotificationPermissionRequested()

    fun isBackgroundRestricted(): Boolean
}

class AndroidStartEnvironment(context: Context) : StartEnvironment {
    private val context = context.applicationContext
    private val preferences = this.context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !preferences.getBoolean(KEY_ASKED, false) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    override fun markNotificationPermissionRequested() = preferences.edit { putBoolean(KEY_ASKED, true) }

    override fun isBackgroundRestricted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.getSystemService<ActivityManager>()?.isBackgroundRestricted == true

    private companion object {
        const val PREFERENCES = "start_flow"
        const val KEY_ASKED = "notificationPermissionAsked"
    }
}

/** Where an unfinished start waits for the user. Survives recreation of the activity. */
data class StartProgress(val timerId: String, val stage: Stage) : Serializable {
    enum class Stage { AwaitingPermission, BackgroundWarning }
}

/** What to do next on the way to a started workout. */
internal sealed interface StartDecision {
    data object RequestPermission : StartDecision

    data object WarnBackgroundRestricted : StartDecision

    data object Start : StartDecision
}

internal fun decideStart(
    needsPermission: Boolean,
    backgroundRestricted: Boolean,
    permissionHandled: Boolean,
    warningHandled: Boolean,
): StartDecision = when {
    needsPermission && !permissionHandled -> StartDecision.RequestPermission
    backgroundRestricted && !warningHandled -> StartDecision.WarnBackgroundRestricted
    else -> StartDecision.Start
}

/**
 * The steps between a tap on Start and the service: the notification permission (asked once, the workout
 * starts even if denied), then a warning when the system restricts the app in the background.
 * UI-free: the Compose holder only forwards results and shows the warning dialog from [progress].
 */
class WorkoutStartFlow(
    private val environment: StartEnvironment,
    private val prepare: suspend (timerId: String) -> PreparedWorkout?,
    private val launch: (PreparedWorkout) -> Unit,
    private val requestNotificationPermission: () -> Unit,
    initial: StartProgress? = null,
) {
    private val mutableProgress = MutableStateFlow(initial)

    /** Null while nothing waits for the user. */
    val progress: StateFlow<StartProgress?> = mutableProgress

    suspend fun begin(timerId: String) = advance(timerId, permissionHandled = false, warningHandled = false)

    /** The result of the system dialog. A denial changes nothing: the workout runs, only its notification is hidden. */
    suspend fun onNotificationPermissionResult() {
        val waiting = mutableProgress.value ?: return
        if (waiting.stage != StartProgress.Stage.AwaitingPermission) return
        advance(waiting.timerId, permissionHandled = true, warningHandled = false)
    }

    suspend fun confirmBackgroundWarning() {
        val waiting = mutableProgress.value ?: return
        if (waiting.stage != StartProgress.Stage.BackgroundWarning) return
        advance(waiting.timerId, permissionHandled = true, warningHandled = true)
    }

    fun cancel() {
        mutableProgress.value = null
    }

    private suspend fun advance(timerId: String, permissionHandled: Boolean, warningHandled: Boolean) {
        val decision = decideStart(
            needsPermission = environment.needsNotificationPermission(),
            backgroundRestricted = environment.isBackgroundRestricted(),
            permissionHandled = permissionHandled,
            warningHandled = warningHandled,
        )
        when (decision) {
            StartDecision.RequestPermission -> {
                mutableProgress.value = StartProgress(timerId, StartProgress.Stage.AwaitingPermission)
                environment.markNotificationPermissionRequested()
                requestNotificationPermission()
            }
            StartDecision.WarnBackgroundRestricted ->
                mutableProgress.value = StartProgress(timerId, StartProgress.Stage.BackgroundWarning)
            StartDecision.Start -> {
                mutableProgress.value = null
                prepare(timerId)?.let(launch)
            }
        }
    }
}
