package model

import kotlinx.serialization.Serializable

@Serializable
class Recipe {
    var uid: String = ""

    var cookingInstructions: MutableList<String> = mutableListOf()

    var notes: MutableList<String> = mutableListOf()

    var description: String = ""

    var dietaryHabit: EatingHabit = EatingHabit.OMNIVORE

    var shoppingIngredients: MutableList<ShoppingIngredient> = mutableListOf()

    var materials: List<String> = listOf()

    var name: String = ""

    var pageInCookbook: Long = 0

    var price: Range = Range.MEDIUM

    var season: List<Season> = listOf()

    private val foodIntolerance: List<FoodIntolerance> = listOf()

    var source: String = ""

    var time: TimeRange = TimeRange.MEDIUM

    var skillLevel: Range = Range.MEDIUM

    var type: List<RecipeType> = listOf()

    fun matchesSearchQuery(
        searchText: String,
        filterForEatingHabit: EatingHabit? = null,
        filterForFoodIntolerance: Set<FoodIntolerance> = emptySet(),
        filterForPrice: Range?,
        filterForTime: TimeRange?,
        filterForRecipeType: RecipeType?,
        filterForSkillLevel: Range?,
        filterForSeason: Season?
    ): Boolean {
        // Apply Filters
        if (filterForEatingHabit != null && !dietaryHabit.matches(filterForEatingHabit))
            return false
        if (!foodIntolerance.containsAll(filterForFoodIntolerance)) {
            return false
        }
        if (filterForPrice != null && price != filterForPrice)
            return false

        if (filterForTime != null && time != filterForTime)
            return false

        if (filterForSkillLevel != null && skillLevel != filterForSkillLevel)
            return false

        if (filterForRecipeType != null && !type.contains(filterForRecipeType))
            return false

        if (filterForSeason != null && !season.contains(filterForSeason))
            return false

        // Filter Text
        if (name.contains(searchText, ignoreCase = true))
            return true
        if (description.contains(searchText))
            return true
        if (pageInCookbook.toString() == searchText)
            return true
        return false
    }

}



