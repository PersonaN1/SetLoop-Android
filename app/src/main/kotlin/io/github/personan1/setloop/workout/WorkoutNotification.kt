package io.github.personan1.setloop.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.getSystemService
import io.github.personan1.setloop.MainActivity
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.cappedName
import kotlin.time.Duration

/**
 * The one ongoing notification, mirroring the iOS Live Activity (TECH T10, DESIGN V10): colored with the step color,
 * the countdown runs system-side as a chronometer, so it is rebuilt only on step change, pause and resume.
 *
 * Not promoted via `Notification.ProgressStyle` (Android 16 Live Updates): promoted notifications must not be
 * colorized, and the step color is the point of this one.
 */
class WorkoutNotification(private val context: Context) {
    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.workout_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.workout_channel_description)
            setShowBadge(false)
        }
        requireNotNull(context.getSystemService<NotificationManager>()).createNotificationChannel(channel)
    }

    /** [remaining] is the time left in the step now; the chronometer counts down to `now + remaining`. */
    fun build(state: WorkoutUiState.Active, options: WorkoutOptions, remaining: Duration): Notification {
        val texts = WorkoutTexts(context)
        val stepName = state.step.step.name.cappedName
        val details = listOf(texts.position(state.step), state.next?.let(texts::next).orEmpty())
            .filter { it.isNotEmpty() }
        val title = if (state.isPaused) {
            "$stepName - ${context.getString(R.string.workout_paused)} ${WorkoutFormat.time(remaining)}"
        } else {
            stepName
        }
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setContentTitle(title)
            .setContentText(details.firstOrNull())
            .setSubText(state.timerName.cappedName)
            .setStyle(Notification.BigTextStyle().bigText(details.joinToString("\n")))
            .setColor(state.color.argb.toInt())
            .setColorized(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(if (options.showOnLockScreen) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_SECRET)
            .setContentIntent(openAppIntent())
            .addAction(pauseAction(state.isPaused))
            .addAction(action(android.R.drawable.ic_media_next, R.string.action_skip, WorkoutService.ACTION_SKIP))
        if (state.isPaused) {
            builder.setShowWhen(false)
        } else {
            builder
                .setWhen(System.currentTimeMillis() + remaining.inWholeMilliseconds)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun pauseAction(isPaused: Boolean): Notification.Action = if (isPaused) {
        action(android.R.drawable.ic_media_play, R.string.action_resume, WorkoutService.ACTION_TOGGLE_PAUSE)
    } else {
        action(android.R.drawable.ic_media_pause, R.string.action_pause, WorkoutService.ACTION_TOGGLE_PAUSE)
    }

    private fun action(icon: Int, title: Int, intentAction: String): Notification.Action {
        val intent = Intent(context, WorkoutService::class.java).setAction(intentAction)
        val pending = PendingIntent.getService(
            context,
            intentAction.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Action.Builder(Icon.createWithResource(context, icon), context.getString(title), pending).build()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val CHANNEL_ID = "workout"
        const val NOTIFICATION_ID = 1
    }
}
