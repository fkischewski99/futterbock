package model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import view.shared.list.ListItem

@Serializable
@Entity(tableName = "ingredients")
class Ingredient : ListItem<Ingredient> {
    @PrimaryKey var uid: String = ""
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
    var expirationDateInDays: Int? = null
    var intolerances = listOf<FoodIntolerance>()

    override fun getListItemTitle(): String {
        return name
    }

    override fun getSubtitle(): String {
        return category
    }

    override fun getItem(): Ingredient {
        return this
    }
}