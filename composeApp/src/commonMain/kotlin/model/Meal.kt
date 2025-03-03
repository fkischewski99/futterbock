package model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import view.shared.list.ListItem

@Serializable
data class Meal(
    val uid: String = "",  // Default value for uid
    var day: Instant,      // day is required and can't be null
    var mealType: MealType = MealType.MITTAG,
    var recipeSelections: List<RecipeSelection> = emptyList() // Immutable list
) : ListItem<Meal> {

    override fun getTitle(): String {
        return mealType.name // Custom title using 'meal_type' and 'day'
    }

    override fun getSubtitle(): String {
        // Custom subtitle logic based on 'recipe_selections' or any other properties
        return recipeSelections.joinToString(", ") { it.selectedRecipeName }
    }

    override fun getItem(): Meal {
        return this
    }

    override fun getId(): String {
        return uid
    }
}