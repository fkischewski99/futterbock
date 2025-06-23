package model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import view.shared.list.ListItem

@Serializable
data class Meal(
    val uid: String = "",
    var day: Instant,
    var mealType: MealType = MealType.MITTAG,
    var recipeSelections: List<RecipeSelection> = emptyList(),
    var guestCount: Int = 0
) : ListItem<Meal> {

    override fun getListItemTitle(): String {
        return mealType.name
    }

    override fun getSubtitle(): String {
        return recipeSelections.joinToString(", ") { it.selectedRecipeName }
    }

    override fun getItem(): Meal {
        return this
    }
}