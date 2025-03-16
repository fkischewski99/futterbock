package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = FoodIntoleranceSerializer::class)
enum class FoodIntolerance(val displayName: String) {
    LACTOSE_INTOLERANCE("laktosefrei"),
    FRUCTOSE_INTOLERANCE("fruktosearm"),
    WITHOUT_NUTS("ohne Nüsse"),
    GLUTEN_INTOLERANCE("glutenfrei");

    override fun toString(): String {
        return this.displayName
    }
}


object FoodIntoleranceSerializer : KSerializer<FoodIntolerance> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IngredientUnit", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FoodIntolerance) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): FoodIntolerance {
        val value = decoder.decodeString()
        val entry =
            FoodIntolerance.entries.find { it.name.lowercase() == value.lowercase() || it.displayName.lowercase() == value.lowercase() }
        if (entry != null)
            return entry
        Logger.e("FoodIntolerance with the name: $value has no matching enum")
        return FoodIntolerance.FRUCTOSE_INTOLERANCE
    }
}