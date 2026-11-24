package io.github.personan1.setloop.workout

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.getSystemService
import io.github.personan1.setloop.core.PlannedStep
import io.github.personan1.setloop.core.SoundCue
import io.github.personan1.setloop.core.SoundSynth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Signals, voice and audio focus of a workout (SPEC D5, D6; TECH T6). Nothing is played, and no focus is taken,
 * while the workout is paused or when both sounds and voice are off. Focus is taken for a signal with ducking
 * and given back after a quiet second, so music is ducked around signals and not for the whole workout.
 *
 * Silent mode: with "ignore silent mode" on the signals use `USAGE_ALARM`, which the ringer mode does not mute.
 * Off uses `USAGE_NOTIFICATION_EVENT`: it follows the notification stream, so silent mode and Do Not Disturb mute it.
 *
 * Must be used from the main thread.
 */
class WorkoutAudio(
    private val context: Context,
    private val options: WorkoutOptions,
    private val scope: CoroutineScope,
    private val onInterruption: () -> Unit,
) {
    private val audioManager = requireNotNull(context.getSystemService<AudioManager>())
    private val enabled = options.soundsEnabled || options.voicePrompts
    private val usage = if (options.ignoreSilentMode) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT
    private val soundAttributes = attributes(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(soundAttributes)
        .setOnAudioFocusChangeListener(::onFocusChange)
        .build()
    private val callMonitor = if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        CallModeMonitor(context, audioManager, onInterruption)
    } else {
        null
    }

    /** Rendered off the main thread: the first signal comes right at the start. */
    private val tracks: Deferred<Map<SoundCue, Sound>> = scope.async(Dispatchers.Default) { renderSounds() }
    private val speech: TextToSpeech? = if (options.voicePrompts) TextToSpeech(context, ::onSpeechInit) else null
    private var speechReady = false
    private var pendingSpeech: Pair<String, Duration>? = null

    private var hasFocus = false
    private var isPaused = false
    private var releaseJob: Job? = null
    private val idleActions = mutableListOf<() -> Unit>()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) callMonitor?.start()
    }

    /** "3-2-1": spoken when the voice is on, otherwise a tick. */
    fun countdown(secondsLeft: Int) {
        if (options.voicePrompts) speak(secondsLeft.toString()) else play(SoundCue.Countdown)
    }

    /** The signal for the step type, then the step name. */
    fun stepStarted(step: PlannedStep) {
        play(SoundCue.from(step.type)) { signalLength -> speak(step.step.name, signalLength) }
    }

    fun finished() = play(SoundCue.Finish)

    /** Nothing plays during a pause. */
    fun pause() {
        if (isPaused) return
        isPaused = true
        speech?.stop()
        pendingSpeech = null
        releaseNow()
    }

    fun resume() {
        isPaused = false
    }

    /** Runs [action] once focus is given back, so a final signal is not cut by the end of the service. */
    fun whenIdle(action: () -> Unit) {
        if (releaseJob?.isActive == true) idleActions += action else action()
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) callMonitor?.stop()
        speech?.shutdown()
        releaseNow()
        scope.launch { tracks.await().values.forEach { it.track.release() } }
    }

    private fun play(cue: SoundCue, afterPlayed: (Duration) -> Unit = {}) {
        if (!enabled || isPaused) return
        scope.launch {
            if (options.soundsEnabled) {
                val sound = tracks.await().getValue(cue)
                if (!acquireFocus()) return@launch
                sound.restart()
                scheduleRelease(sound.length + QUIET_BEFORE_RELEASE)
                afterPlayed(sound.length)
            } else {
                afterPlayed(Duration.ZERO)
            }
        }
    }

    private fun speak(text: String, delay: Duration = Duration.ZERO) {
        val engine = speech ?: return
        if (!enabled || isPaused || text.isEmpty()) return
        if (!speechReady) {
            pendingSpeech = text to delay
            return
        }
        if (!acquireFocus()) return
        // The silence stands in for the signal still playing; the speech must not cut it.
        if (delay > Duration.ZERO) {
            engine.playSilentUtterance(delay.inWholeMilliseconds, TextToSpeech.QUEUE_FLUSH, SILENCE_ID)
        }
        val mode = if (delay > Duration.ZERO) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        engine.speak(text, mode, null, SPEECH_ID)
        // Backs up the done callback: if it never comes, focus is still given back.
        scheduleRelease(delay + SPEECH_FALLBACK)
    }

    private fun onSpeechInit(status: Int) {
        val engine = speech ?: return
        if (status != TextToSpeech.SUCCESS) return
        engine.language = Locale.getDefault()
        engine.setAudioAttributes(attributes(AudioAttributes.CONTENT_TYPE_SPEECH))
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit

            override fun onDone(utteranceId: String) {
                if (utteranceId == SPEECH_ID) scope.launch { scheduleRelease(QUIET_BEFORE_RELEASE) }
            }

            @Deprecated("Replaced by onError(String, Int)")
            override fun onError(utteranceId: String) {
                if (utteranceId == SPEECH_ID) scope.launch { scheduleRelease(QUIET_BEFORE_RELEASE) }
            }
        })
        speechReady = true
        pendingSpeech?.let { (text, delay) -> speak(text, delay) }
        pendingSpeech = null
    }

    private fun acquireFocus(): Boolean {
        releaseJob?.cancel()
        if (hasFocus) return true
        hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    private fun scheduleRelease(after: Duration) {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(after)
            releaseNow()
        }
    }

    private fun releaseNow() {
        releaseJob?.cancel()
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
        val actions = idleActions.toList()
        idleActions.clear()
        actions.forEach { it() }
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (isCallMode(audioManager.mode)) onInterruption()
            AudioManager.AUDIOFOCUS_LOSS -> hasFocus = false
        }
    }

    private fun attributes(contentType: Int) =
        AudioAttributes.Builder().setUsage(usage).setContentType(contentType).build()

    private fun renderSounds(): Map<SoundCue, Sound> {
        if (!options.soundsEnabled) return emptyMap()
        return SoundCue.entries.associateWith { cue ->
            val samples = SoundSynth.samples(cue, options.soundSet)
            val pcm = ShortArray(samples.size) { (samples[it] * Short.MAX_VALUE).toInt().toShort() }
            val track = AudioTrack.Builder()
                .setAudioAttributes(soundAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SoundSynth.SAMPLE_RATE.toInt())
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            Sound(track, (samples.size / SoundSynth.SAMPLE_RATE * 1000).toLong().milliseconds)
        }
    }

    /** A static track per cue: a tick must not cut the tail of a step sound. */
    private class Sound(val track: AudioTrack, val length: Duration) {
        fun restart() {
            track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    private companion object {
        val QUIET_BEFORE_RELEASE = 1.seconds
        val SPEECH_FALLBACK = 5.seconds
        const val SILENCE_ID = "silence"
        const val SPEECH_ID = "speech"
    }
}
