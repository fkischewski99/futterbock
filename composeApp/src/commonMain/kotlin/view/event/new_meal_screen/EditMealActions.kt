package view.event.new_meal_screen

import kotlinx.datetime.LocalDate
import model.MealType
import model.ParticipantTime
import model.Recipe
import model.RecipeSelection
import view.event.actions.BaseAction

interface EditMealActions : BaseAction {
    data class SelectRecipe(val recipie: Recipe) : EditMealActions
    data class DeleteRecipe(val recipeSelection: RecipeSelection) : EditMealActions
    data class RemoveEaterFromRecipe(
        val recipeSelection: RecipeSelection,
        val participantTime: ParticipantTime
    ) : EditMealActions

    data class AddEaterToRecipe(
        val recipeSelection: RecipeSelection,
        val participantTime: ParticipantTime
    ) : EditMealActions

    data class ChangeDateOfMeal(val newDayOfMeal: LocalDate) : EditMealActions
    data class ChangeMealType(val mealType: MealType) : EditMealActions
    data class ViewRecipe(val recipeSelection: RecipeSelection) : EditMealActions
    data class UpdateGuestCount(val recipeSelection: RecipeSelection, val guestCount: Int) : EditMealActions

    data object SaveMeal : EditMealActions
}