package io.github.personan1.setloop.shortcuts

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat
import io.github.personan1.setloop.R
import io.github.personan1.setloop.workout.WorkoutController
import io.github.personan1.setloop.workout.WorkoutUiState

/**
 * Target of the static Pause and Resume shortcuts. Shows nothing: it commands the running workout, which
 * lives in this process with the foreground service, and finishes at once.
 */
class WorkoutCommandActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = WorkoutController.instance
        val action = ShortcutIntents.parse(intent)
        action?.shortcutId?.let { ShortcutManagerCompat.reportShortcutUsed(this, it) }
        val active = controller.state.value as? WorkoutUiState.Active
        when (action) {
            ShortcutAction.Pause ->
                if (active != null && !active.isPaused) controller.pause() else toast(R.string.shortcut_error_nothing_to_pause)
            ShortcutAction.Resume ->
                if (active != null && active.isPaused) controller.resume() else toast(R.string.shortcut_error_nothing_to_resume)
            is ShortcutAction.StartTimer, null -> Unit
        }
        finish()
    }

    private val ShortcutAction.shortcutId: String?
        get() = when (this) {
            ShortcutAction.Pause -> "pause"
            ShortcutAction.Resume -> "resume"
            is ShortcutAction.StartTimer -> null
        }

    private fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
