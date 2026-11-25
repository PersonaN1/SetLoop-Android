package io.github.personan1.setloop.workout

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.github.personan1.setloop.core.SoundSynth

/** A static mono 16-bit track holding [samples] from [SoundSynth], ready to `play()`. The caller releases it. */
internal fun staticPcmTrack(samples: FloatArray, attributes: AudioAttributes): AudioTrack {
    val pcm = ShortArray(samples.size) { (samples[it] * Short.MAX_VALUE).toInt().toShort() }
    val track = AudioTrack.Builder()
        .setAudioAttributes(attributes)
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
    return track
}
