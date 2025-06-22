package services.shoppingList

import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import model.IngredientUnit
import model.Participant
import model.RecipeSelection
import model.ShoppingIngredient

//Calculates the Amount of Ingredients that need to be shoppend
class CalculateShoppingList(private val eventRepository: EventRepository) {
    var allParticipants = emptyMap<String, Participant>()

    suspend fun calculate(eventId: String): List<ShoppingIngredient> {
        val shoppingIngredients = eventRepository.getShoppingIngredients(eventId)
        val map = addExistingIngredients(shoppingIngredients)

        // ✅ OPTIMIZED: Fetch all participants once for the entire calculation
        allParticipants = fetchAllParticipantsForEvent(eventId)
        val finalMap = addAllAmounts(eventId, map)

        return finalMap.values.toList().sortedBy { it.ingredient?.name }
    }

    private suspend fun fetchAllParticipantsForEvent(eventId: String): Map<String, Participant> {
        return try {
            val participantTimes =
                eventRepository.getParticipantsOfEvent(eventId, withParticipant = true)
            participantTimes.mapNotNull { participantTime ->
                participantTime.participant?.let { participant ->
                    participant.uid to participant
                }
            }.toMap()
        } catch (e: Exception) {
            Logger.e("Error fetching participants for shopping calculation: ${e.message}")
            emptyMap()
        }
    }


    private suspend fun addAllAmounts(
        eventId: String,
        map: MutableMap<String, ShoppingIngredient>
    ): Map<String, ShoppingIngredient> {
        val listOfMeals = eventRepository.getMealsWithRecipeAndIngredients(eventId)
        val calculatedShoppingIngredients: MutableMap<String, ShoppingIngredient> = map;

        for (meal in listOfMeals) {
            for (recipeSelection in meal.recipeSelections) {
                if (null == recipeSelection.recipe) {
                    continue
                }
                val calculatedMap =
                    calculateAmountsForRecipe(
                        calculatedShoppingIngredients,
                        recipeSelection
                    ).toMutableMap()
                calculatedShoppingIngredients.putAll(calculatedMap)
            }
        }
        return calculatedShoppingIngredients;
    }

    /**
     * Calculates the amount of ingredients for a recipe
     * If multiplier is added it is used, else the size of eater ids is used
     */
    suspend fun calculateAmountsForRecipe(
        existingShoppingIngredients: Map<String, ShoppingIngredient>,
        recipeSelection: RecipeSelection,
        multiplier: Double? = null
    ): Map<String, ShoppingIngredient> {
        val multiplierForIngredients = multiplier ?: getEaterMultiplier(recipeSelection)
        val newIngredientMap = HashMap<String, ShoppingIngredient>()
        for (recipeIngredient in recipeSelection.recipe!!.shoppingIngredients) {
            try {
                Logger.i("Original amount: ${recipeIngredient.amount}")

                val converted = getMetricUnitShoppingIngredient(recipeIngredient)
                val ingredientKey = recipeIngredient.ingredientRef

                val existing = existingShoppingIngredients[ingredientKey]
                val baseAmount = converted.amount * multiplierForIngredients

                converted.amount = baseAmount + (existing?.amount ?: 0.0)

                converted.note = existing?.note ?: converted.note
                newIngredientMap[ingredientKey] = converted

                Logger.i("Added $baseAmount to $ingredientKey, total: ${converted.amount}")

            } catch (e: Exception) {
                println("Error calculating amount for ingredient ${recipeIngredient.ingredient?.name ?: "unknown"}")
                e.printStackTrace()
            }
        }

        return newIngredientMap
    }


    private fun getMetricUnitShoppingIngredient(
        shoppingIngredient: ShoppingIngredient,
    ): ShoppingIngredient {
        val newShoppingIngredient =
            ShoppingIngredient().apply {
                ingredient = shoppingIngredient.ingredient
                unit = shoppingIngredient.unit
                amount = shoppingIngredient.amount
                ingredientRef = shoppingIngredient.ingredientRef
            }
        try {
            when (shoppingIngredient.unit) {
                IngredientUnit.HAFERL -> {
                    newShoppingIngredient.amount =
                        shoppingIngredient.amount * shoppingIngredient.ingredient
                            ?.amountHaferl!!

                    newShoppingIngredient.unit =
                        shoppingIngredient.ingredient?.unitHaferl!!
                }

                IngredientUnit.TEELOEFFEL -> {
                    newShoppingIngredient.amount =
                        shoppingIngredient.amount * shoppingIngredient.ingredient
                            ?.amountTeaspoon!!

                    newShoppingIngredient.unit =
                        shoppingIngredient.ingredient?.unitTablespoon!!

                }

                IngredientUnit.ESSLOEFFEL -> {
                    newShoppingIngredient.amount =
                        shoppingIngredient.amount * shoppingIngredient.ingredient
                            ?.amountTablespoon!!

                    newShoppingIngredient.unit =
                        shoppingIngredient.ingredient?.unitTablespoon!!

                }

                else -> {
                    //noting to do
                }
            }
        } catch (e: NullPointerException) {
            Logger.e("No metric Item for: ${shoppingIngredient.ingredient}")
        }
        return newShoppingIngredient
    }

    private suspend fun getEaterMultiplier(recipeSelection: RecipeSelection): Double {

        return recipeSelection.eaterIds.map { eaterId -> getMultiplierForParticipant(eaterId) }
            .sum()
    }

    private suspend fun getMultiplierForParticipant(participantId: String): Double {
        val participant =
            allParticipants[participantId] ?: eventRepository.getParticipantById(participantId)
        if (participant?.birthdate == null) {
            return 1.0
        }
        return getFactorByBirthdate(participant.birthdate!!)
    }

    private fun getFactorByBirthdate(birthdate: Instant): Double {
        // Calculate the age from the birthdate
        val currentDate = Clock.System.now()
        val age = birthdate.periodUntil(
            other = currentDate,
            timeZone = TimeZone.currentSystemDefault()
        ).years

        return when {
            age in 0..4 -> 0.4
            age in 5..10 -> 0.7
            age in 11..14 -> 1.0
            age in 15..23 -> 1.2
            age >= 24 -> 1.0
            else -> throw IllegalArgumentException("Age must be a positive number")
        }
    }

    // Adds existing ingredients with a amount of 0, to not lose the description
    private fun
            addExistingIngredients(
        listOfShoppingIngredients: List<ShoppingIngredient>,
    ): HashMap<String, ShoppingIngredient> {
        val newMap = HashMap<String, ShoppingIngredient>()
        for (shopIngredient in listOfShoppingIngredients) {
            val ingredientKey =
                if (shopIngredient.nameEnteredByUser != "") shopIngredient.nameEnteredByUser else shopIngredient.ingredientRef
            val newShoppingIngredient = ShoppingIngredient().apply {
                uid = shopIngredient.uid
                ingredient = shopIngredient.ingredient
                ingredientRef = shopIngredient.ingredientRef
                unit = shopIngredient.unit
                shoppingDone = shopIngredient.shoppingDone
                nameEnteredByUser = shopIngredient.nameEnteredByUser
                note = shopIngredient.note
            }
            shopIngredient.amount = 0.0
            newMap[ingredientKey] = newShoppingIngredient
        }
        return newMap;
    }

}