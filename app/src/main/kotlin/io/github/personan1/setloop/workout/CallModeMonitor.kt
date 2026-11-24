package io.github.personan1.setloop.workout

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Calls the callback when the audio mode turns to a call, without READ_PHONE_STATE.
 * Audio focus is held only around signals, so focus loss alone would miss a call that rings in silence.
 */
@RequiresApi(Build.VERSION_CODES.S)
class CallModeMonitor(
    private val context: Context,
    private val audioManager: AudioManager,
    private val onCall: () -> Unit,
) {
    private val listener = AudioManager.OnModeChangedListener { mode -> if (isCallMode(mode)) onCall() }

    fun start() = audioManager.addOnModeChangedListener(context.mainExecutor, listener)

    fun stop() = audioManager.removeOnModeChangedListener(listener)
}

fun isCallMode(mode: Int): Boolean =
    mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
