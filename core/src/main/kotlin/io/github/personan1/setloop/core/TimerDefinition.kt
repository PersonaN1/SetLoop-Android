package io.github.personan1.setloop.core

import java.text.BreakIterator
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/** Step type. Defines color, sound and how the step is counted in history (SPEC D2, DESIGN V2). */
@Serializable
enum class StepType(val category: Category) {
    @SerialName("prepare") Prepare(Category.Other),
    @SerialName("workA") WorkA(Category.Work),
    @SerialName("workB") WorkB(Category.Work),
    @SerialName("workC") WorkC(Category.Work),
    @SerialName("rest") Rest(Category.Rest),
    @SerialName("cooldown") Cooldown(Category.Other);

    enum class Category { Work, Rest, Other }

    /** The raw value used in the shared JSON format, e.g. "workA". */
    val rawValue: String get() = name.replaceFirstChar { it.lowercase() }

    companion object {
        fun fromRawValue(rawValue: String): StepType? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

/** Swift `UUID` encodes as an uppercase string; any case is accepted on decode. */
object UuidSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString().uppercase())

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable
data class Step(
    @Serializable(with = UuidSerializer::class) val id: UUID = UUID.randomUUID(),
    val name: String,
    /** Seconds, 1..5999 (SPEC D18). */
    val duration: Int,
    val type: StepType,
) {
    companion object
}

@Serializable
data class Block(
    @Serializable(with = UuidSerializer::class) val id: UUID = UUID.randomUUID(),
    val steps: List<Step>,
    /** Cycles, 1..99. */
    val repeats: Int = 1,
)

/**
 * Full timer structure, stored as one JSON value (TECH T5), wire-compatible with the iOS `Codable` output.
 * Timer = [Prepare] -> Sets xN (rest between) -> Blocks xM -> Steps -> [Cooldown] (SPEC D2).
 */
@Serializable
data class TimerDefinition(
    val version: Int = CURRENT_VERSION,
    val prepare: Step? = null,
    val blocks: List<Block>,
    /** 1..99. */
    val sets: Int = 1,
    /** Rest between sets; used only when `sets > 1`. */
    val restBetweenSets: Step? = null,
    val cooldown: Step? = null,
    /** Removes only the final rest at the very end of the workout (SPEC D18). */
    val skipLastRest: Boolean = false,
) {
    sealed class ValidationError : Exception() {
        data object NoSteps : ValidationError()
        data object TooManyBlocks : ValidationError()
        data class TooManyStepsInBlock(val blockIndex: Int) : ValidationError()
        data class StepDurationOutOfRange(val stepId: UUID) : ValidationError()
        data object RepeatsOutOfRange : ValidationError()

        /** Only the skipped last rest is left: there is nothing to run. */
        data object EmptyPlan : ValidationError()
    }

    sealed class MergeError : Exception() {
        /** Steps come from different blocks, are not neighbors, or nothing is selected. */
        data object NotContiguous : MergeError()

        /** The selection already is a whole block. */
        data object WholeBlock : MergeError()

        data object TooManyBlocks : MergeError()
    }

    /** @throws ValidationError */
    fun validate() {
        if (blocks.none { it.steps.isNotEmpty() }) throw ValidationError.NoSteps
        if (blocks.size > Limits.maxBlocks) throw ValidationError.TooManyBlocks
        if (sets !in Limits.repeats) throw ValidationError.RepeatsOutOfRange
        blocks.forEachIndexed { index, block ->
            if (block.steps.size > Limits.maxStepsPerBlock) throw ValidationError.TooManyStepsInBlock(index)
            if (block.repeats !in Limits.repeats) throw ValidationError.RepeatsOutOfRange
        }
        val allSteps = blocks.flatMap { it.steps } + listOfNotNull(prepare, restBetweenSets, cooldown)
        allSteps.firstOrNull { it.duration !in Limits.stepDuration }?.let {
            throw ValidationError.StepDurationOutOfRange(it.id)
        }
        if (stepCount == 0) throw ValidationError.EmptyPlan
    }

    /**
     * Renames the steps for which [rename] returns a new name. Returns the same instance when nothing changed,
     * so callers can compare with `!==` (Swift returns a "changed" flag from a mutating method).
     */
    fun renameSteps(rename: (String) -> String?): TimerDefinition {
        var changed = false
        fun apply(step: Step): Step {
            val name = rename(step.name)
            if (name == null || name == step.name) return step
            changed = true
            return step.copy(name = name)
        }

        val result = copy(
            prepare = prepare?.let(::apply),
            blocks = blocks.map { block -> block.copy(steps = block.steps.map(::apply)) },
            restBetweenSets = restBetweenSets?.let(::apply),
            cooldown = cooldown?.let(::apply),
        )
        return if (changed) result else this
    }

    companion object {
        const val CURRENT_VERSION = 1

        /** Matches Swift `JSONEncoder`/`JSONDecoder`: nil optionals are omitted, unknown keys are ignored. */
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        fun fromJson(text: String): TimerDefinition = json.decodeFromString(serializer(), text)
    }

    fun toJson(): String = json.encodeToString(serializer(), this)
}

object Limits {
    /** The list shows no counter; a hint appears only when adding is refused (SPEC D9). */
    const val maxTimers = 100

    /** At the limit, saving a workout removes the oldest record (SPEC D10). */
    const val maxHistoryRecords = 3000
    const val maxBlocks = 10
    const val maxStepsPerBlock = 30
    val stepDuration = 1..5999
    val repeats = 1..99

    /** Timer, step and history names. Keeps the Live Activity state well under its 4 KB limit. */
    const val maxNameLength = 60
}

/** The name cut to `Limits.maxNameLength` characters (grapheme clusters, as in Swift). */
val String.cappedName: String
    get() {
        val boundaries = BreakIterator.getCharacterInstance().also { it.setText(this) }
        var end = boundaries.first()
        repeat(Limits.maxNameLength) {
            val next = boundaries.next()
            if (next == BreakIterator.DONE) return this
            end = next
        }
        return if (boundaries.next() == BreakIterator.DONE) this else substring(0, end)
    }
