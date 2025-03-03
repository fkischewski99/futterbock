package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind

@Serializable(with = IngredientUnitSerializer::class)
enum class IngredientUnit(val display: String) {
    HAFERL("Haferl"),
    STUECK("Stk."),
    ESSLOEFFEL("EL"),
    TEELOEFFEL("TL"),
    MILLILITER("ml"),
    LITER("L"),
    KILOGRAMM("kg"),
    GRAMM("g"),
    PRISE("Prise"),
    BUND("Bund"),
    ZEHE("Zehe"),
    KNOLLE("Knolle"),
    PAECKCHEN("Pck."),
    OTHER("");

    // Zehe und Knolle Aufnehmen --> Umrechnungsfaktor 10

    companion object {
        fun default(): IngredientUnit = ESSLOEFFEL // Default value
    }
}

object IngredientUnitSerializer : KSerializer<IngredientUnit> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IngredientUnit", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IngredientUnit) {
        encoder.encodeString(value.display)
    }

    override fun deserialize(decoder: Decoder): IngredientUnit {
        val value = decoder.decodeString()
        val entry =
            IngredientUnit.entries.find { it.display.uppercase() == value.uppercase() || it.name.uppercase() == value.uppercase() }
        if (entry != null)
            return entry
        if (value.uppercase() == "ZEHEN")
            return IngredientUnit.ZEHE
        if (value.uppercase() == "KNOLLEN")
            return IngredientUnit.KNOLLE
        Logger.e("IngredientUnit with the name: $value has no matching enum")
        return IngredientUnit.OTHER // Return default value if not found
    }
}