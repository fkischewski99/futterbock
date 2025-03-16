package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = EatingHabitSerializer::class)
enum class EatingHabit(private val displayName: String) {
    VEGAN("Vegan"),
    VEGETARISCH("Vegetarisch"),
    PESCETARISCH("Pescetarisch"),
    OMNIVORE("Omnivore");

    fun matches(other: EatingHabit): Boolean {
        return other.ordinal >= ordinal
    }

    override fun toString(): String {
        return this.displayName
    }
}


object EatingHabitSerializer : KSerializer<EatingHabit> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EatingHabit", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EatingHabit) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): EatingHabit {
        val value = decoder.decodeString()
        val entry = EatingHabit.entries.find { it.name.lowercase() == value.lowercase() }
        if (entry != null)
            return entry
        Logger.e("EatingHabit with the name: $value has no matching enum")
        return EatingHabit.OMNIVORE
    }
}