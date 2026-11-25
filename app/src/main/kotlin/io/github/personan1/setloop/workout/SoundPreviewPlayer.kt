package io.github.personan1.setloop.workout

import android.media.AudioAttributes
import android.media.AudioTrack
import io.github.personan1.setloop.core.SoundCue
import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.core.SoundSynth
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays the step-change signal of a sound set when it is picked in Settings (SPEC D22). A new preview cuts the
 * previous one. The stream follows "ignore silent mode" as the workout does, so the preview is heard exactly
 * when the real signal would be. Main thread only.
 */
class SoundPreviewPlayer(private val scope: CoroutineScope) {
    private var track: AudioTrack? = null
    private var job: Job? = null

    fun play(set: SoundSet, ignoreSilentMode: Boolean) {
        job?.cancel()
        stop()
        job = scope.launch {
            val samples = withContext(Dispatchers.Default) { SoundSynth.samples(SoundCue.Work, set) }
            val next = staticPcmTrack(samples, attributes(ignoreSilentMode))
            track = next
            next.play()
            try {
                delay((samples.size / SoundSynth.SAMPLE_RATE * MILLIS_PER_SECOND).toLong().milliseconds + TAIL)
            } finally {
                if (track === next) stop()
            }
        }
    }

    fun release() {
        job?.cancel()
        stop()
    }

    private fun stop() {
        track?.release()
        track = null
    }

    private fun attributes(ignoreSilentMode: Boolean): AudioAttributes = AudioAttributes.Builder()
        .setUsage(if (ignoreSilentMode) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private companion object {
        const val MILLIS_PER_SECOND = 1000
        val TAIL = 200.milliseconds
    }
}
