package model

import kotlinx.serialization.Serializable

@Serializable
class Ingredient {
    var uid: String = ""
    var name: String = ""
    var amountHaferl: Double? = null
    var unitHaferl: IngredientUnit? = null
    var amountTablespoon: Double? = null
    var unitTablespoon: IngredientUnit? = null
    var amountTeaspoon: Double? = null
    var unitTeaspoon: IngredientUnit? = null
    var amountWeight: Double? = null
    var unitWeight: IngredientUnit? = null
    var category: String = ""
}