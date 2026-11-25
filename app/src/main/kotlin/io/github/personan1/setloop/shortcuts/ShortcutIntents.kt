package io.github.personan1.setloop.shortcuts

import android.content.Context
import android.content.Intent
import io.github.personan1.setloop.MainActivity

/** What a launcher shortcut asks for (SPEC D15). */
sealed interface ShortcutAction {
    data class StartTimer(val timerId: String) : ShortcutAction

    data object Pause : ShortcutAction

    data object Resume : ShortcutAction
}

/** Builds and reads shortcut intents. Intents come from outside the app, so everything is validated here. */
object ShortcutIntents {
    const val ACTION_START_TIMER = "io.github.personan1.setloop.action.START_TIMER"
    const val ACTION_PAUSE = "io.github.personan1.setloop.action.PAUSE"
    const val ACTION_RESUME = "io.github.personan1.setloop.action.RESUME"
    const val EXTRA_TIMER_ID = "io.github.personan1.setloop.extra.TIMER_ID"

    private const val MAX_ID_LENGTH = 64
    private val idPattern = Regex("[A-Za-z0-9-]+")

    /** Opens the app and starts the timer; needs the screen, sound and notification flow of the app. */
    fun startTimer(context: Context, timerId: String): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_START_TIMER)
            .putExtra(EXTRA_TIMER_ID, timerId)

    /** Null for a foreign or malformed intent. Whether the timer exists is checked when it is started. */
    fun parse(intent: Intent?): ShortcutAction? = parse(intent?.action, intent?.getStringExtra(EXTRA_TIMER_ID))

    fun parse(action: String?, timerId: String?): ShortcutAction? = when (action) {
        ACTION_START_TIMER -> timerId?.takeIf(::isValidId)?.let(ShortcutAction::StartTimer)
        ACTION_PAUSE -> ShortcutAction.Pause
        ACTION_RESUME -> ShortcutAction.Resume
        else -> null
    }

    private fun isValidId(id: String) = id.length <= MAX_ID_LENGTH && idPattern.matches(id)
}
