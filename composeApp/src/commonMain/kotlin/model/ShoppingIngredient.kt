package model

import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import view.shared.list.ListItem
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Serializable
class ShoppingIngredient() : ListItem<ShoppingIngredient> {
    var uid: String = ""
    var ingredientRef: String = ""
    var nameEnteredByUser: String = ""

    @Transient
    var ingredient: Ingredient? = null
    var amount: Double = 0.0;
    var unit: IngredientUnit = IngredientUnit.GRAMM
    var title: String? = null
    var shoppingDone: Boolean = false;
    var note: String = "";
    var source: Source = Source.COMPUTED

    override fun getListItemTitle(): String {
        return ingredient?.name ?: nameEnteredByUser
    }

    override fun getSubtitle(): String {
        return ""
    }

    override fun getItem(): ShoppingIngredient {
        return this
    }

    override fun toString(): String {
        if (nameEnteredByUser != "") {
            return nameEnteredByUser
        }
        val noteString = if (note != "") ("($note)") else ""
        return getFormatedAmount() + " " + (ingredient?.name ?: "") + " " + noteString
    }

    fun getFormatedAmount(): String {
        if (amount == 0.0) {
            return ""
        }
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
                return "${amount.format(1, true)} ${if (amount <= 1.0) "Zehe" else "Zehen"}"
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
        val pow = 10.0.pow(fracDigits.toDouble())
        var result = (round(this * pow) / pow)
        if (roundUp) {
            // always round up
            result = ceil(result)
        }
        
        // For numbers >= 100, don't show decimal places
        val actualFracDigits = if (result >= 100.0) 0 else fracDigits
        
        // Format the number
        val formattedNumber = if (actualFracDigits == 0 || result.toInt().toDouble() == result) {
            result.toInt().toString()
        } else {
            // Use manual formatting since String.format is not available on all platforms
            val integerPart = result.toInt()
            val multiplier = 10.0.pow(actualFracDigits.toDouble())
            val fractionalPart = ((result - integerPart) * multiplier).toInt()
            val fractionalStr = fractionalPart.toString().padStart(actualFracDigits, '0')
            "$integerPart.$fractionalStr"
        }
        
        // Add thousand separators (dots) for numbers >= 1000
        return if (result >= 1000.0) {
            formatNumberWithThousandSeparators(formattedNumber)
        } else {
            formattedNumber
        }
    }
    
    private fun formatNumberWithThousandSeparators(number: String): String {
        val parts = number.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) ".${parts[1]}" else ""
        
        // Add dots every 3 digits from right to left
        val reversedInteger = integerPart.reversed()
        val chunked = reversedInteger.chunked(3).joinToString(".")
        val formattedInteger = chunked.reversed()
        
        return formattedInteger + decimalPart
    }
}