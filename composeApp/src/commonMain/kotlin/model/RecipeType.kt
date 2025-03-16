package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RecipeTypeSerializer::class)
enum class RecipeType(val displayName: String) {
    BREAKFAST("Frühstück"),
    BREAD_TIME("Brotzeit"),
    SALAD("Salat"),
    NOODLE("Nudelgerichte"),
    RICE("Reisgerichte"),
    POTATO("Kartoffelgerichte"),
    SOUPS("Suppen und Eintöpfe"),
    DRINKS("Getränke"),
    SNACK("Nachtisch und Snack"),
    BAKING("Backanhänger"),
    FIRECOOKING("Feuerküche"),
    IDEAS_FOR_GROUP_SESSION("Ideen für die Gruppenstunde");
    //LUNCH("Mittagessen"),
    //DINNER("Abendessen");

    override fun toString(): String {
        return this.displayName
    }
}

object RecipeTypeSerializer : KSerializer<RecipeType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RecipeType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RecipeType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): RecipeType {
        val value = decoder.decodeString()
        val entry = RecipeType.entries.find { it.displayName.lowercase() == value.lowercase() }
        if (entry != null)
            return entry
        Logger.e("RecipeType with the name: $value has no matching enum")
        return RecipeType.IDEAS_FOR_GROUP_SESSION
    }
}