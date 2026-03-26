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
enum class FoodIntolerance(val displayName: String, val aliases: List<String> = emptyList()) {
    LACTOSE_INTOLERANCE("laktosefrei", listOf("laktose")),
    FRUCTOSE_INTOLERANCE("fruktosefrei", listOf("fruktosearm", "fruktose")),
    WITHOUT_NUTS("ohne Nüsse", listOf("nussfrei", "nüsse")),
    GLUTEN_INTOLERANCE("glutenfrei", listOf("gluten"));

    fun matches(value: String): Boolean =
        name.equals(value, ignoreCase = true)
                || displayName.equals(value, ignoreCase = true)
                || aliases.any { it.equals(value, ignoreCase = true) }

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
        val value = decoder.decodeString().trim()
        if (value.isBlank()) {
            Logger.w("FoodIntolerance with blank name, skipping")
            return FoodIntolerance.FRUCTOSE_INTOLERANCE
        }
        val entry = FoodIntolerance.entries.find { it.matches(value) }
        if (entry != null)
            return entry
        Logger.e("FoodIntolerance with the name: $value has no matching enum")
        return FoodIntolerance.FRUCTOSE_INTOLERANCE
    }
}