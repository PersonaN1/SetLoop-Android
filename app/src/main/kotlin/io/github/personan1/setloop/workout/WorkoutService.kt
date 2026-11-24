package io.github.personan1.setloop.workout

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.getSystemService
import io.github.personan1.setloop.core.TimerDefinition
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service (type specialUse) that keeps a workout running with the screen off.
 * Started from the UI when the user taps Start, stops itself when the workout ends. It only hosts the
 * effects (notification, wake lock, audio); the workout itself lives in [WorkoutController].
 */
class WorkoutService : Service() {
    private val controller = WorkoutController.instance
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notification: WorkoutNotification
    private lateinit var wakeLock: PowerManager.WakeLock
    private var audio: WorkoutAudio? = null
    private var haptics: WorkoutHaptics? = null
    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notification = WorkoutNotification(this)
        notification.createChannel()
        wakeLock = requireNotNull(getSystemService<PowerManager>())
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { setReferenceCounted(false) }
        controller.attach(Effects(), PreferencesSnapshotStore(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> controller.start(requestFrom(intent))
            ACTION_TOGGLE_PAUSE -> controller.togglePause()
            ACTION_SKIP -> controller.skip()
            // A sticky restart after the process was killed.
            null -> controller.restore()
        }
        // A finished workout stops the service itself, once its last signal has played.
        if (controller.state.value == WorkoutUiState.Idle) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        if (wakeLock.isHeld) wakeLock.release()
        audio?.release()
        controller.detach()
        scope.cancel()
        super.onDestroy()
    }

    private fun requestFrom(intent: Intent) = WorkoutRequest(
        definition = TimerDefinition.fromJson(requireNotNull(intent.getStringExtra(EXTRA_DEFINITION))),
        timerName = intent.getStringExtra(EXTRA_NAME).orEmpty(),
        options = WorkoutOptions.fromJson(requireNotNull(intent.getStringExtra(EXTRA_OPTIONS))),
        startedAtEpochMillis = System.currentTimeMillis(),
    )

    private inner class Effects : WorkoutEffects {
        private var options = WorkoutOptions()

        override fun onWorkoutStarted(options: WorkoutOptions) {
            this.options = options
            audio?.release()
            audio = WorkoutAudio(this@WorkoutService, options, scope) {
                if (options.pauseOnInterruption) controller.pause()
            }
            haptics = WorkoutHaptics(this@WorkoutService, options.vibration)
        }

        override fun onSignal(signal: WorkoutSignal) {
            when (signal) {
                is WorkoutSignal.Countdown -> {
                    haptics?.tick()
                    audio?.countdown(signal.secondsLeft)
                }
                is WorkoutSignal.StepStarted -> {
                    haptics?.step()
                    audio?.stepStarted(signal.step)
                }
                WorkoutSignal.Finished -> {
                    haptics?.step()
                    audio?.finished()
                }
            }
        }

        override fun onStateChanged(state: WorkoutUiState) {
            when (state) {
                is WorkoutUiState.Active -> show(state, options)
                is WorkoutUiState.Finished -> finish()
                WorkoutUiState.Idle -> Unit
            }
        }
    }

    private fun show(state: WorkoutUiState.Active, options: WorkoutOptions) {
        val built = notification.build(state, options, state.remaining(ElapsedRealtimeClock.now()))
        if (isForeground) {
            requireNotNull(getSystemService<NotificationManager>()).notify(WorkoutNotification.NOTIFICATION_ID, built)
        } else {
            startForeground(built)
            isForeground = true
        }
        if (state.isPaused) {
            wakeLock.releaseIfHeld()
            audio?.pause()
        } else {
            // Lasts for the rest of the workout; renewed on every state change, so a stuck service cannot hold it forever.
            wakeLock.acquire((state.workoutRemaining + WAKE_LOCK_MARGIN).inWholeMilliseconds)
            audio?.resume()
        }
    }

    private fun finish() {
        wakeLock.releaseIfHeld()
        // After the controller call returns, so the final signal has been handed to the audio.
        scope.launch(Dispatchers.Main) {
            val closing = audio
            if (closing == null) shutdown() else closing.whenIdle(::shutdown)
        }
    }

    private fun shutdown() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    private fun startForeground(built: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(WorkoutNotification.NOTIFICATION_ID, built, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(WorkoutNotification.NOTIFICATION_ID, built)
        }
    }

    private fun PowerManager.WakeLock.releaseIfHeld() {
        if (isHeld) release()
    }

    companion object {
        const val ACTION_START = "io.github.personan1.setloop.workout.START"
        const val ACTION_TOGGLE_PAUSE = "io.github.personan1.setloop.workout.TOGGLE_PAUSE"
        const val ACTION_SKIP = "io.github.personan1.setloop.workout.SKIP"
        private const val EXTRA_DEFINITION = "definition"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_OPTIONS = "options"
        private const val WAKE_LOCK_TAG = "SetLoop:workout"
        private val WAKE_LOCK_MARGIN = 1.minutes

        /**
         * Starts a workout. Call while the app is in front (the user tapped Start): foreground services
         * cannot be started from the background on Android 12+.
         */
        fun start(context: Context, definition: TimerDefinition, timerName: String, options: WorkoutOptions) {
            val intent = Intent(context, WorkoutService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DEFINITION, definition.toJson())
                .putExtra(EXTRA_NAME, timerName)
                .putExtra(EXTRA_OPTIONS, options.toJson())
            context.startForegroundService(intent)
        }
    }
}
