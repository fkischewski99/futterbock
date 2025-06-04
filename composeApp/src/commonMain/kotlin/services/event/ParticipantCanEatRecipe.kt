package services.event

import co.touchlab.kermit.Logger
import data.EventRepository
import model.FoodIntolerance
import model.Participant
import model.ParticipantTime
import model.Recipe
import model.RecipeSelection

class ParticipantCanEatRecipe(private val eventRepository: EventRepository) {

    private fun memberAndRecipeSelectionAreAvailable(
        member: ParticipantTime,
        recipeSelection: RecipeSelection
    ): Boolean {
        if (member.participant == null || recipeSelection.recipe == null) {
            Logger.e("Member or recipe is null, cannot check if participant can eat recipe");
            return false;
        }
        return true
    }

    suspend fun getErrorMessageForParticipant(
        member: ParticipantTime,
        recipeSelection: RecipeSelection
    ): String? {

        if (!memberAndRecipeSelectionAreAvailable(member, recipeSelection)) {
            return null
        }

        val participant = member.participant!!
        val recipe = recipeSelection.recipe!!

        if (eatingHabitOfParticipantDoesNotMatchRecipe(participant, recipe)) {
            return "Essgewohnheit stimmt nicht überein"
        }
        val ingredientId = doesParticipantHasAnAllergyForIngredientInRecipe(participant, recipe)
        if (ingredientId != null) {
            return "Teilnehmer ist allergisch gegen eine der Zutaten: " + eventRepository.getIngredientById(
                ingredientId = ingredientId
            ).name
        }
        val doesParticipantHasAnAnIntolerance =
            doesParticipantHasAnAnIntolerance(participant, recipe)
        if (doesParticipantHasAnAnIntolerance != null) {
            return "Teilnehmer hat eine Unverträglichkeit: " + doesParticipantHasAnAnIntolerance.displayName
        }
        return null
    }

    fun canParticipantEatRecipe(
        member: ParticipantTime,
        recipeSelection: RecipeSelection
    ): Boolean {
        if (!memberAndRecipeSelectionAreAvailable(member, recipeSelection)) {
            return true
        }

        val participant = member.participant!!
        val recipe = recipeSelection.recipe!!

        if (eatingHabitOfParticipantDoesNotMatchRecipe(participant, recipe)) {
            return false
        }
        if (doesParticipantHasAnAllergyForIngredientInRecipe(participant, recipe) != null) {
            return false
        }
        if (doesParticipantHasAnAnIntolerance(participant, recipe) != null) {
            return false
        }
        return true
    }


    private fun doesParticipantHasAnAnIntolerance(
        participant: Participant,
        recipe: Recipe
    ): FoodIntolerance? {
        participant.intolerances.forEach {
            if (!recipe.foodIntolerances.contains(it)) {
                return it
            }
        }
        return null
    }

    private fun doesParticipantHasAnAllergyForIngredientInRecipe(
        participant: Participant,
        recipe: Recipe
    ): String? {
        participant.allergies.forEach {
            if (recipe.shoppingIngredients.map { shoppingIngredient -> shoppingIngredient.ingredientRef }
                    .contains(it)) {
                return it
            }
        }
        return null
    }

    private fun eatingHabitOfParticipantDoesNotMatchRecipe(
        participant: Participant,
        recipe: Recipe
    ) = participant.eatingHabit < recipe.dietaryHabit
}