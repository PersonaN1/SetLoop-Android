package io.github.personan1.setloop.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Built-in sound sets (SPEC D5, TECH T6), picked by ear on the tone lab page. */
@Serializable
enum class SoundSet {
    @SerialName("glass") Glass,
    @SerialName("marimba") Marimba,
    @SerialName("note") Note,
}

/** What a signal announces. The step sound depends on the step type, not on the step. */
enum class SoundCue {
    Countdown, Work, Rest, Other, Finish;

    companion object {
        fun from(type: StepType): SoundCue = when (type.category) {
            StepType.Category.Work -> Work
            StepType.Category.Rest -> Rest
            StepType.Category.Other -> Other
        }
    }
}

/**
 * Synthesizes the signals in code in the style of iOS system sounds: soft attack, natural decay,
 * mid register and a little room (TECH T6). No audio files, no licenses.
 * Output is mono PCM as floats in -1..1 at [SAMPLE_RATE]; playback is up to the caller.
 */
object SoundSynth {
    const val SAMPLE_RATE = 44_100.0

    /** Peak level of every sound, leaving headroom for the mixer. */
    const val PEAK = 0.8f

    /** Everything three semitones below the lab page's default, picked by ear. */
    private val transpose = 2.0.pow(-3.0 / 12)

    /** Mono samples at [SAMPLE_RATE], normalized to [PEAK]. */
    fun samples(cue: SoundCue, set: SoundSet): FloatArray {
        val (voice, notes) = score(set, cue)
        return render(voice, notes.map { Note(it.frequency * transpose, it.start) })
    }

    internal data class Note(val frequency: Double, val start: Double)

    private fun render(voice: Voice, notes: List<Note>): FloatArray {
        val length = (notes.maxOfOrNull { it.start } ?: 0.0) + voice.length
        val dry = FloatArray(ceil(length * SAMPLE_RATE).toInt())
        for (note in notes) {
            val first = (note.start * SAMPLE_RATE).roundToInt()
            for (i in 0 until max(dry.size - first, 0)) {
                dry[first + i] += voice.sample(note.frequency, i / SAMPLE_RATE).toFloat()
            }
        }
        val output = reverb(dry)
        // A cut tail never clicks.
        val fade = (0.15 * SAMPLE_RATE).toInt()
        for (i in 0 until min(fade, output.size)) {
            output[output.size - 1 - i] *= i.toFloat() / fade.toFloat()
        }
        val maxLevel = output.maxOfOrNull { abs(it) } ?: 0f
        if (maxLevel <= 0f) return output
        val gain = PEAK / maxLevel
        return FloatArray(output.size) { output[it] * gain }
    }

    /** Small Schroeder reverb: 4 combs and 2 allpasses, 18 % wet, 0.4 s tail. */
    private fun reverb(x: FloatArray): FloatArray {
        val n = x.size + (0.4 * SAMPLE_RATE).toInt()
        val wet = FloatArray(n)
        for (delay in intArrayOf(1116, 1188, 1277, 1356)) {
            val buffer = FloatArray(delay)
            var index = 0
            for (k in 0 until n) {
                val out = buffer[index]
                buffer[index] = (if (k < x.size) x[k] else 0f) + out * 0.72f
                index = (index + 1) % delay
                wet[k] += out / 4
            }
        }
        for (delay in intArrayOf(556, 441)) {
            val buffer = FloatArray(delay)
            var index = 0
            for (k in 0 until n) {
                val stored = buffer[index]
                val input = wet[k]
                buffer[index] = input + stored * 0.5f
                index = (index + 1) % delay
                wet[k] = stored - input
            }
        }
        return FloatArray(n) { k -> (if (k < x.size) x[k] else 0f) + 0.18f * wet[k] }
    }

    /** A sum of decaying partials, with an optional pitch drop at the start (woodblock). */
    internal class Voice(val partials: List<Partial>, val attack: Double, val drop: Double = 0.0) {
        /** Frequency ratio, amplitude, decay time constant in seconds. */
        class Partial(val ratio: Double, val amplitude: Double, val decay: Double)

        /** Until about -40 dB, at most 2.5 s; the reverb adds its own tail. */
        val length: Double get() = min(2.5, (partials.maxOfOrNull { it.decay } ?: 0.0) * 4.6)

        fun sample(frequency: Double, t: Double): Double {
            val pitch = if (drop > 0) frequency * (1 + drop * exp(-t / 0.005)) else frequency
            var sum = 0.0
            for (p in partials) sum += p.amplitude * exp(-t / p.decay) * sin(2 * PI * pitch * p.ratio * t)
            return min(1.0, t / attack) * sum
        }

        /** Same timbre with a shorter tail. */
        fun shortened(factor: Double) = Voice(partials.map { Partial(it.ratio, it.amplitude, it.decay * factor) }, attack, drop)

        companion object {
            // Tails are 0.3 of the lab page's, shortened by ear.
            val glass = Voice(
                listOf(Partial(1.0, 1.0, 0.9), Partial(2.76, 0.3, 0.25), Partial(5.4, 0.12, 0.1), Partial(8.9, 0.05, 0.05)),
                attack = 0.003,
            ).shortened(0.3)
            val marimba = Voice(
                listOf(Partial(1.0, 1.0, 0.4), Partial(3.93, 0.3, 0.06), Partial(9.2, 0.06, 0.02)),
                attack = 0.002,
            ).shortened(0.3)
            val note = Voice(
                listOf(Partial(1.0, 1.0, 0.45), Partial(2.0, 0.35, 0.2), Partial(3.0, 0.1, 0.1)),
                attack = 0.006,
            ).shortened(0.3)
            val tink = Voice(listOf(Partial(1.0, 1.0, 0.12), Partial(2.76, 0.3, 0.04)), attack = 0.001)
            val wood = Voice(listOf(Partial(1.0, 1.0, 0.035), Partial(2.6, 0.4, 0.015)), attack = 0.001, drop = 0.3)
            val tock = Voice(listOf(Partial(1.0, 1.0, 0.05), Partial(2.0, 0.15, 0.02)), attack = 0.001)
        }
    }

    private object Pitch {
        const val A4 = 440.0
        const val C5 = 523.25
        const val C_SHARP5 = 554.37
        const val E5 = 659.25
        const val G5 = 783.99
        const val A5 = 880.0
        const val C6 = 1046.5
    }

    /** Work goes up, rest goes down, finish is a rising run. */
    private fun score(set: SoundSet, cue: SoundCue): Pair<Voice, List<Note>> {
        fun notes(vararg pairs: Pair<Double, Double>) = pairs.map { Note(it.first, it.second) }
        return when (set) {
            SoundSet.Glass -> when (cue) {
                SoundCue.Countdown -> Voice.tink to notes(1319.0 to 0.0)
                SoundCue.Work -> Voice.glass to notes(Pitch.E5 to 0.0, Pitch.A5 to 0.11)
                SoundCue.Rest -> Voice.glass to notes(Pitch.A5 to 0.0, Pitch.E5 to 0.11)
                SoundCue.Other -> Voice.glass to notes(Pitch.G5 to 0.0)
                SoundCue.Finish -> Voice.glass to notes(Pitch.C5 to 0.0, Pitch.E5 to 0.12, Pitch.G5 to 0.24, Pitch.C6 to 0.36)
            }

            SoundSet.Marimba -> when (cue) {
                SoundCue.Countdown -> Voice.wood to notes(1100.0 to 0.0)
                SoundCue.Work -> Voice.marimba to notes(Pitch.C5 to 0.0, Pitch.G5 to 0.1)
                SoundCue.Rest -> Voice.marimba to notes(Pitch.G5 to 0.0, Pitch.C5 to 0.1)
                SoundCue.Other -> Voice.marimba to notes(Pitch.E5 to 0.0)
                SoundCue.Finish -> Voice.marimba to notes(Pitch.C5 to 0.0, Pitch.E5 to 0.1, Pitch.G5 to 0.2, Pitch.C6 to 0.3)
            }

            SoundSet.Note -> when (cue) {
                SoundCue.Countdown -> Voice.tock to notes(800.0 to 0.0)
                SoundCue.Work -> Voice.note to notes(Pitch.A4 to 0.0, Pitch.E5 to 0.14)
                SoundCue.Rest -> Voice.note to notes(Pitch.E5 to 0.0, Pitch.A4 to 0.14)
                SoundCue.Other -> Voice.note to notes(Pitch.C5 to 0.0)
                SoundCue.Finish -> Voice.note to notes(Pitch.A4 to 0.0, Pitch.C_SHARP5 to 0.12, Pitch.E5 to 0.24, Pitch.A5 to 0.36)
            }
        }
    }
}
