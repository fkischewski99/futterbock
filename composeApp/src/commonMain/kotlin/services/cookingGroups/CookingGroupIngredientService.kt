package services.cookingGroups

import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalDate
import model.*
import services.shoppingList.CalculateShoppingList
import view.shared.HelperFunctions.Companion.generateRandomStringId

/**
 * Service for calculating ingredient distribution per cooking group.
 * Reuses existing shopping list calculation logic by creating modified meals
 * with cooking group participants as eaters.
 */
class CookingGroupIngredientService(
    private val calculateShoppingList: CalculateShoppingList
) {

    companion object {
        const val OTHERS_GROUP_NAME = "Andere"
        const val GUESTS_GROUP_NAME = "Gäste"
    }

    /**
     * Calculates ingredient requirements for each cooking group on a specific day.
     *
     * @param eventId The event ID
     * @param date The specific date to calculate for
     * @param meals All meals for the specified date
     * @param participants All event participants with their cooking group assignments
     * @return Map of cooking group name to their required ingredients
     */
    suspend fun calculateIngredientsPerCookingGroup(
        eventId: String,
        date: LocalDate,
        meals: List<Meal>,
        participants: List<ParticipantTime>
    ): Map<String, List<ShoppingIngredient>> {

        // Group participants by cooking group
        val participantsByCookingGroup = groupParticipantsByCookingGroup(participants)

        Logger.d("Calculating ingredients for ${participantsByCookingGroup.size} cooking groups on $date")
        Logger.d("Cooking groups: ${participantsByCookingGroup.keys.joinToString(", ")}")

        val result = mutableMapOf<String, List<ShoppingIngredient>>()

        // Calculate ingredients for each cooking group (participants only)
        for ((cookingGroup, groupParticipants) in participantsByCookingGroup) {
            try {
                Logger.d("Processing cooking group '$cookingGroup' with ${groupParticipants.size} participants")

                // Create modified meals with only the cooking group participants as eaters
                val modifiedMeals =
                    createMealsForCookingGroup(meals, groupParticipants, includeGuests = false)

                if (modifiedMeals.isNotEmpty()) {
                    // Use existing shopping list calculation logic
                    val dailyShoppingList = calculateShoppingList.calculateIngredientsPerDay(
                        eventId = eventId,
                        meals = modifiedMeals,
                    )

                    result[cookingGroup] = dailyShoppingList[date]?.values?.toList() ?: emptyList()
                    Logger.d("Calculated ${result[cookingGroup]?.size} ingredients for cooking group '$cookingGroup'")
                } else {
                    result[cookingGroup] = emptyList()
                    Logger.d("No meals found for cooking group '$cookingGroup'")
                }

            } catch (e: Exception) {
                Logger.e("Error calculating ingredients for cooking group '$cookingGroup'", e)
                result[cookingGroup] = emptyList()
            }
        }

        // Calculate ingredients for guests separately
        try {
            Logger.d("Processing guests group")

            // Create modified meals with only guests (no participant eaters)
            val guestMeals = createMealsForGuests(meals)

            if (guestMeals.isNotEmpty()) {
                // Use existing shopping list calculation logic with empty participants list
                val dailyShoppingList = calculateShoppingList.calculateIngredientsPerDay(
                    eventId = eventId,
                    meals = guestMeals,
                )

                result[GUESTS_GROUP_NAME] = dailyShoppingList[date]?.values?.toList() ?: emptyList()
                Logger.d("Calculated ${result[GUESTS_GROUP_NAME]?.size} ingredients for guests")
            } else {
                result[GUESTS_GROUP_NAME] = emptyList()
                Logger.d("No guest meals found")
            }

        } catch (e: Exception) {
            Logger.e("Error calculating ingredients for guests", e)
            result[GUESTS_GROUP_NAME] = emptyList()
        }

        return result
    }

    /**
     * Groups participants by their cooking group assignment.
     * Participants without a cooking group are assigned to "Andere" (Others).
     */
    private fun groupParticipantsByCookingGroup(participants: List<ParticipantTime>): Map<String, List<ParticipantTime>> {
        return participants.groupBy { participant ->
            participant.cookingGroup.ifBlank { OTHERS_GROUP_NAME }
        }
    }

    /**
     * Creates modified copies of meals where only the specified cooking group participants
     * are included as eaters in the recipe selections.
     */
    private fun createMealsForCookingGroup(
        originalMeals: List<Meal>,
        cookingGroupParticipants: List<ParticipantTime>,
        includeGuests: Boolean = false
    ): List<Meal> {
        val participantIds = cookingGroupParticipants.map { it.participantRef }.toSet()

        return originalMeals.map { originalMeal ->
            // Create modified recipe selections with only cooking group participants
            val modifiedRecipeSelections = originalMeal.recipeSelections.map { originalSelection ->
                createModifiedRecipeSelection(originalSelection, participantIds, includeGuests)
            }.filter { selection ->
                // Only include recipe selections that have eaters from this cooking group or guests
                selection.eaterIds.isNotEmpty() || (includeGuests && selection.guestCount > 0)
            }

            // Create new meal with modified recipe selections
            originalMeal.copy(
                uid = generateRandomStringId(), // New ID to avoid conflicts
                recipeSelections = modifiedRecipeSelections
            )
        }.filter { meal ->
            // Only include meals that have recipe selections with eaters
            meal.recipeSelections.isNotEmpty()
        }
    }

    /**
     * Creates modified copies of meals where only guests are included (no participant eaters).
     */
    private fun createMealsForGuests(originalMeals: List<Meal>): List<Meal> {
        return originalMeals.map { originalMeal ->
            // Create modified recipe selections with only guests
            val modifiedRecipeSelections = originalMeal.recipeSelections.map { originalSelection ->
                RecipeSelection().apply {
                    uid = generateRandomStringId()
                    recipeRef = originalSelection.recipeRef
                    selectedRecipeName = originalSelection.selectedRecipeName
                    recipe = originalSelection.recipe
                    eaterIds = mutableSetOf() // No participant eaters
                    guestCount = originalSelection.guestCount // Only guests
                }
            }.filter { selection ->
                // Only include recipe selections that have guests
                selection.guestCount > 0
            }

            // Create new meal with modified recipe selections
            originalMeal.copy(
                uid = generateRandomStringId(), // New ID to avoid conflicts
                recipeSelections = modifiedRecipeSelections
            )
        }.filter { meal ->
            // Only include meals that have recipe selections with guests
            meal.recipeSelections.isNotEmpty()
        }
    }

    /**
     * Creates a modified recipe selection that only includes eaters from the specified cooking group.
     */
    private fun createModifiedRecipeSelection(
        originalSelection: RecipeSelection,
        cookingGroupParticipantIds: Set<String>,
        includeGuests: Boolean = false
    ): RecipeSelection {
        return RecipeSelection().apply {
            uid = generateRandomStringId()
            recipeRef = originalSelection.recipeRef
            selectedRecipeName = originalSelection.selectedRecipeName
            recipe = originalSelection.recipe

            // Only include eaters that belong to this cooking group
            eaterIds =
                originalSelection.eaterIds.intersect(cookingGroupParticipantIds).toMutableSet()

            // Include guests only if specified
            guestCount = if (includeGuests) originalSelection.guestCount else 0
        }
    }

    /**
     * Data class representing ingredient distribution for a specific cooking group.
     */
    data class CookingGroupIngredients(
        val cookingGroupName: String,
        val participantCount: Int,
        val guestCount: Int,
        var ingredients: List<ShoppingIngredient>
    )

    /**
     * Convenience method that returns structured data for UI consumption.
     */
    suspend fun getCookingGroupIngredientsForDay(
        eventId: String,
        date: LocalDate,
        meals: List<Meal>,
        participants: List<ParticipantTime>
    ): List<CookingGroupIngredients> {
        val ingredientsByGroup =
            calculateIngredientsPerCookingGroup(eventId, date, meals, participants)
        val participantsByCookingGroup = groupParticipantsByCookingGroup(participants)

        // Calculate total guest count from all meals
        val totalGuestCount = meals.flatMap { it.recipeSelections }
            .sumOf { it.guestCount }

        return ingredientsByGroup.map { (cookingGroup, ingredients) ->
            CookingGroupIngredients(
                cookingGroupName = cookingGroup,
                participantCount = if (cookingGroup == GUESTS_GROUP_NAME) 0 else participantsByCookingGroup[cookingGroup]?.size
                    ?: 0,
                guestCount = if (cookingGroup == GUESTS_GROUP_NAME) totalGuestCount else 0,
                ingredients = ingredients
            )
        }.sortedBy { it.cookingGroupName }
    }
}