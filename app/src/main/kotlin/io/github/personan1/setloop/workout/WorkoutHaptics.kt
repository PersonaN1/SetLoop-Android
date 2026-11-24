package io.github.personan1.setloop.workout

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

/**
 * Short tick on the countdown, a longer buzz on a new step (SPEC D5).
 * Uses the same alarm/notification usage as the signals: the default touch usage is dropped when the user
 * turns off touch feedback and may be suppressed with the screen off.
 */
class WorkoutHaptics(context: Context, private val enabled: Boolean, ignoreSilentMode: Boolean) {
    private val vibrator = context.getSystemService<Vibrator>()

    private val attributes: VibrationAttributes? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        VibrationAttributes.createForUsage(
            if (ignoreSilentMode) VibrationAttributes.USAGE_ALARM else VibrationAttributes.USAGE_NOTIFICATION,
        )
    } else {
        null
    }

    private val legacyAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(if (ignoreSilentMode) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun tick() = vibrate(TICK)

    fun step() = vibrate(STEP)

    private fun vibrate(effect: VibrationEffect) {
        val vibrator = vibrator ?: return
        if (!enabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, requireNotNull(attributes))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, legacyAttributes)
        }
    }

    private companion object {
        val TICK: VibrationEffect = VibrationEffect.createOneShot(30, 120)
        val STEP: VibrationEffect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
    }
}
