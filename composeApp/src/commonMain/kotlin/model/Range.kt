package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RangeSerializer::class)
enum class Range(
    val displayName: String
) {
    HIGH("hoch"),
    MEDIUM("mittel"),
    LOW("niedrig");

    override fun toString(): String {
        return this.displayName
    }
}

object RangeSerializer : KSerializer<Range> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IngredientUnit", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Range) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): Range {
        val value = decoder.decodeString()
        val entry = Range.entries.find { it.name == value || it.displayName == value }
        if (entry != null)
            return entry
        Logger.e("Range with the name: $value has no matching enum")
        return Range.MEDIUM
    }
}