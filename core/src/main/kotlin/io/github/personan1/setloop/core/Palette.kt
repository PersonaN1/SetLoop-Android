package io.github.personan1.setloop.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@Serializable
private class RgbSurrogate(val hex: Int)

object RgbSerializer : KSerializer<RGB> {
    override val descriptor: SerialDescriptor = RgbSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RGB) =
        encoder.encodeSerializableValue(RgbSurrogate.serializer(), RgbSurrogate(value.hex))

    override fun deserialize(decoder: Decoder): RGB =
        RGB(decoder.decodeSerializableValue(RgbSurrogate.serializer()).hex)
}

/** 24-bit RGB color, e.g. `RGB(0xE8452C)`. Serialized as `{"hex": 15222060}`. */
@Serializable(with = RgbSerializer::class)
class RGB(hex: Int) {
    val hex: Int = hex and 0xFFFFFF

    val red: Double get() = ((hex shr 16) and 0xFF) / 255.0
    val green: Double get() = ((hex shr 8) and 0xFF) / 255.0
    val blue: Double get() = (hex and 0xFF) / 255.0

    /** Brighter variant for text on the always-black Dynamic Island (DESIGN V10). */
    val lightened: RGB
        get() {
            val maxC = max(red, max(green, blue))
            val minC = min(red, min(green, blue))
            if (maxC <= 0) return this
            val saturation = (maxC - minC) / maxC * 0.85
            fun channel(value: Double): Double {
                val relative = if (maxC == minC) 1.0 else (value - minC) / (maxC - minC)
                return 1 - saturation * (1 - relative)
            }
            return fromComponents(channel(red), channel(green), channel(blue))
        }

    /** `0xAARRGGBB` with full opacity, ready for Android `Color`. */
    val argb: Long get() = 0xFF000000L or hex.toLong()

    override fun equals(other: Any?): Boolean = other is RGB && other.hex == hex

    override fun hashCode(): Int = hex

    override fun toString(): String = "RGB(0x%06X)".format(hex)

    companion object {
        fun fromComponents(red: Double, green: Double, blue: Double): RGB {
            fun byte(value: Double): Int = (min(max(value, 0.0), 1.0) * 255).roundToInt()
            return RGB(byte(red) shl 16 or (byte(green) shl 8) or byte(blue))
        }
    }
}

/** The 12 colors available for step types (DESIGN V2). */
object Palette {
    val colors: List<RGB> = listOf(
        0xE8452C, 0xD63C7E, 0x9C2BB0, 0x7B4FD6, 0x4B55C9, 0x2F6BDB,
        0x0E7C9E, 0x0F8A8A, 0x1E8E52, 0x5F8F1A, 0xB86200, 0x5B6470,
    ).map(::RGB)
}

/**
 * Global type -> color mapping, set in Settings -> Step colors (SPEC D17).
 *
 * Stored as `{"colors": {"workA": {"hex": ...}}}`. Older iOS builds wrote the enum-keyed dictionary
 * as a flat `[key, value, key, value...]` array; both are read, and unknown types are skipped.
 */
@Serializable(with = StepColorsSerializer::class)
data class StepColors(val colors: Map<StepType, RGB>) {
    operator fun get(type: StepType): RGB = colors[type] ?: DEFAULT.colors.getValue(type)

    /** Rest and work must stay distinguishable: the rest color can't be picked for work and vice versa (DESIGN V8). */
    fun isAllowed(color: RGB, type: StepType): Boolean = when (type.category) {
        StepType.Category.Work -> color != this[StepType.Rest]
        StepType.Category.Rest -> listOf(StepType.WorkA, StepType.WorkB, StepType.WorkC).none { this[it] == color }
        StepType.Category.Other -> true
    }

    companion object {
        val DEFAULT = StepColors(
            mapOf(
                StepType.Prepare to RGB(0x5B6470),
                StepType.WorkA to RGB(0xE8452C),
                StepType.WorkB to RGB(0x9C2BB0),
                StepType.WorkC to RGB(0xB86200),
                StepType.Rest to RGB(0x1E8E52),
                StepType.Cooldown to RGB(0x5B6470),
            ),
        )
    }
}

object StepColorsSerializer : KSerializer<StepColors> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StepColors")

    override fun serialize(encoder: Encoder, value: StepColors) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("StepColors needs JSON")
        val keyed = buildJsonObject {
            put("colors", buildJsonObject {
                for ((type, color) in value.colors) {
                    put(type.rawValue, jsonEncoder.json.encodeToJsonElement(RgbSerializer, color))
                }
            })
        }
        jsonEncoder.encodeJsonElement(keyed)
    }

    override fun deserialize(decoder: Decoder): StepColors {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("StepColors needs JSON")
        val root = jsonDecoder.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("StepColors must be an object")
        val json: Json = jsonDecoder.json
        val colors = mutableMapOf<StepType, RGB>()
        when (val stored = root["colors"]) {
            is JsonObject -> for ((key, element) in stored) {
                val color = json.decodeFromJsonElement(RgbSerializer, element)
                StepType.fromRawValue(key)?.let { colors[it] = color }
            }
            is JsonArray -> {
                if (stored.size % 2 != 0) throw SerializationException("Legacy StepColors array must hold pairs")
                for (index in stored.indices step 2) {
                    val key = json.decodeFromJsonElement(String.serializer(), stored[index])
                    val color = json.decodeFromJsonElement(RgbSerializer, stored[index + 1])
                    StepType.fromRawValue(key)?.let { colors[it] = color }
                }
            }
            else -> throw SerializationException("StepColors needs a \"colors\" object or array")
        }
        return StepColors(colors)
    }
}
