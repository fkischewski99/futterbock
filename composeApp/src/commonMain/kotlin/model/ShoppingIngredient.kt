package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import view.shared.list.ListItem
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Serializable
class ShoppingIngredient() : ListItem<ShoppingIngredient> {
    var ingredientRef: String = ""

    @Transient
    var ingredient: Ingredient? = null
    var amount: Double = 0.0;
    var unit: IngredientUnit = IngredientUnit.GRAMM
    var title: String? = null
    var shoppingDone: Boolean = false;
    var note: String = "";

    override fun getListItemTitle(): String {
        return ingredient?.name ?: ""
    }

    override fun getSubtitle(): String {
        return ""
    }

    override fun getItem(): ShoppingIngredient {
        return this
    }

    override fun getId(): String {
        return ""
    }

    override fun toString(): String {
        val noteString = if (note != "") ("($note)") else ""
        return getFormatedAmount() + " " + (ingredient?.name ?: "") + " " + noteString
    }

    fun getFormatedAmount(): String {
        return convertUnitAndAmount(
            amount,
            unit
        )
    }

    private fun convertUnitAndAmount(amount: Double, unit: IngredientUnit): String {
        if (unit == IngredientUnit.ZEHE) {
            if (amount == 1.0) {
                return "${amount.format(1)} ${unit.display}"
            }
            if (amount < 10)
                return "${amount.format(0, true)} ${if (amount == 1.0) "Zehe" else "Zehen"}"
            val newAmount = amount / 10
            return formatKnolle(newAmount)
        }
        if (unit == IngredientUnit.KNOLLE) {
            return formatKnolle(amount)
        }
        if (unit != IngredientUnit.MILLILITER && unit != IngredientUnit.GRAMM) {
            return "${amount.format(1)} ${unit.display}"
        }
        if (amount < 1000) {
            return "${amount.format(1)} ${unit.display}"
        }
        if (unit == IngredientUnit.MILLILITER) {
            val newAmount = amount / 1000;
            return "${newAmount.format(1)} ${IngredientUnit.LITER.display}"
        }
        if (unit == IngredientUnit.GRAMM) {
            val newAmount = amount / 1000;
            return "${newAmount.format(1)} ${IngredientUnit.KILOGRAMM.display}"
        }
        return "${amount.format(1)} ${unit.display}"

    }

    private fun formatKnolle(amount: Double): String {
        if (amount == 1.0) {
            return "${amount.format(1)} ${IngredientUnit.KNOLLE.display}"
        }
        return "${amount.format(1)} Knollen"
    }

    private fun Double.format(fracDigits: Int, roundUp: Boolean = false): String {
        val pow = 10.0.pow(fracDigits.toDouble());
        var result = (round(this * pow) / pow)
        if (roundUp) {
            result = result.roundToInt().toDouble()
        }
        if (result.toInt().toDouble() == result) {
            return result.toInt().toString()
        }
        return result.toString()
    }
}