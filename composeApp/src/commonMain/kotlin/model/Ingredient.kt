package model

import kotlinx.serialization.Serializable
import view.shared.list.ListItem

@Serializable
class Ingredient : ListItem<Ingredient> {
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

    override fun getListItemTitle(): String {
        return name
    }

    override fun getSubtitle(): String {
        return category
    }

    override fun getItem(): Ingredient {
        return this
    }

    override fun getId(): String {
        return ""
    }
}