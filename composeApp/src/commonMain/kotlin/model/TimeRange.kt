package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TimeRangeSerializer::class)
enum class TimeRange(
    val displayName: String
) {
    SHORT("kurz"),
    MEDIUM("mittel"),
    LONG("lang");

    override fun toString(): String {
        return this.displayName
    }
}

object TimeRangeSerializer : KSerializer<TimeRange> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IngredientUnit", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TimeRange) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): TimeRange {
        val value = decoder.decodeString()
        val entry = TimeRange.entries.find { it.name == value || it.displayName == value }
        if (entry != null)
            return entry
        Logger.e("TimeRange with the name: $value has no matching enum")
        return TimeRange.MEDIUM
    }
}