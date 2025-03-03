package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SeasonDeserializer::class)
enum class Season(val displayName: String) {
    SPRING("Frühling"),
    SUMMER_EARLY("Frühsommer"),
    SUMMER_LATE("Spätsommer"),
    AUTUMN("Herbst"),
    WINTER("Winter");

    override fun toString(): String {
        return this.displayName
    }
}

object SeasonDeserializer : KSerializer<Season> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IngredientUnit", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Season) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): Season {
        val value = decoder.decodeString()
        val entry = Season.entries.find { it.name == value || it.displayName == value }
        if (entry != null)
            return entry
        Logger.e("TimeRange with the name: $value has no matching enum")
        return Season.SUMMER_LATE
    }
}