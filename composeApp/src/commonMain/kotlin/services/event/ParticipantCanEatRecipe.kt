package services.event

import data.EventRepository
import model.ParticipantTime
import model.RecipeSelection

class ParticipantCanEatRecipe(private val eventRepository: EventRepository) {

    suspend fun getErrorMessageForParticipant(
        member: ParticipantTime,
        recipeSelection: RecipeSelection
    ): String? {
        member.participant?.let { participant ->
            recipeSelection.recipe?.let { recipe ->
                if (participant.eatingHabit < recipe.dietaryHabit) {
                    return "Essgewohnheit stimmt nicht überein"
                }
            }
            participant.allergies.forEach {
                if (recipeSelection.recipe?.shoppingIngredients?.map { shoppingIngredient -> shoppingIngredient.ingredientRef }
                        ?.contains(it) == true) {
                    return "Teilnehmer ist allergisch gegen eine der Zutaten: " + eventRepository.getIngredientById(
                        ingredientId = it
                    ).name
                }
            }
            participant.intolerances.forEach {
                if (recipeSelection.recipe?.foodIntolerances?.contains(it) == false) {
                    return "Teilnehmer hat eine Unverträglichkeit: " + it.displayName
                }
            }
        }
        return null
    }

    fun canParticipantEatRecipe(
        member: ParticipantTime,
        recipeSelection: RecipeSelection
    ): Boolean {
        member.participant?.let { participant ->
            recipeSelection.recipe?.let { recipe ->
                if (participant.eatingHabit < recipe.dietaryHabit) {
                    return false
                }
            }
            participant.allergies.forEach {
                if (recipeSelection.recipe?.shoppingIngredients?.map { shoppingIngredient -> shoppingIngredient.ingredientRef }
                        ?.contains(it) == true) {
                    return false
                }
            }
            participant.intolerances.forEach {
                if (recipeSelection.recipe?.foodIntolerances?.contains(it) == false) {
                    return false
                }
            }
        }
        return true
    }
}