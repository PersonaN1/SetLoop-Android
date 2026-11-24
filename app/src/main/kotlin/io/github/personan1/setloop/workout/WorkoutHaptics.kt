package io.github.personan1.setloop.workout

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

/** Short tick on the countdown, a longer buzz on a new step (SPEC D5). */
class WorkoutHaptics(context: Context, private val enabled: Boolean) {
    private val vibrator = context.getSystemService<Vibrator>()

    fun tick() = vibrate(TICK)

    fun step() = vibrate(STEP)

    private fun vibrate(effect: VibrationEffect) {
        if (enabled && vibrator?.hasVibrator() == true) vibrator.vibrate(effect)
    }

    private companion object {
        val TICK: VibrationEffect = VibrationEffect.createOneShot(30, 120)
        val STEP: VibrationEffect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
    }
}
