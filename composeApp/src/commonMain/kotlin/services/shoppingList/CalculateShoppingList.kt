package services.shoppingList

import androidx.compose.runtime.toMutableStateMap
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import model.IngredientUnit
import model.MultiDayShoppingList
import model.Participant
import model.RecipeSelection
import model.ShoppingIngredient
import view.shared.HelperFunctions

//Calculates the Amount of Ingredients that need to be shoppend
class CalculateShoppingList(private val eventRepository: EventRepository) {
    var allParticipants = mutableMapOf<String, Participant>()
    private val dayAssignmentAlgorithm = DayAssignmentAlgorithm()

    suspend fun calculate(eventId: String): List<ShoppingIngredient> {
        // Get existing ingredients from multiday shopping list
        val existingMultiDayList = eventRepository.getMultiDayShoppingList(eventId)
        val existingMap = addExistingIngredientsFromMultiDay(existingMultiDayList)

        // ✅ OPTIMIZED: Fetch all participants once for the entire calculation
        allParticipants = fetchAllParticipantsForEvent(eventId)
        val finalMap = addAllAmounts(eventId, existingMap.toMutableMap())

        return finalMap.values.toList().sortedBy { it.ingredient?.name }
    }

    /**
     * Calculates multi-day shopping lists with ingredient expiration optimization
     *
     * @param eventId The event ID
     * @param saveToRepository Whether to save the result to the repository (default: true)
     * @return MultiDayShoppingList with ingredients assigned to optimal purchase days
     */
    suspend fun calculateMultiDay(
        eventId: String,
        saveToRepository: Boolean = true
    ): MultiDayShoppingList {
        Logger.i("Starting multi-day shopping list calculation for event $eventId")

        try {
            // Get event details for date range
            val event = eventRepository.getEventById(eventId)
            if (event == null) {
                Logger.e("Event not found: $eventId")
                return MultiDayShoppingList(eventId, emptyMap())
            }

            val eventStartDate = HelperFunctions.getLocalDate(event.from)

            val existingMultiDayList = eventRepository.getMultiDayShoppingList(eventId)
            val existingMap = addExistingIngredientsFromMultiDay(existingMultiDayList)

            // Fetch all participants once for the entire calculation
            allParticipants = fetchAllParticipantsForEvent(eventId)

            // Get all meals with their ingredients
            val meals = eventRepository.getMealsWithRecipeAndIngredients(eventId)

            // Calculate ingredients per day (maintaining day-by-day context)
            val ingredientsPerDay = calculateIngredientsPerDay(eventId, meals, existingMap)

            // Apply day assignment algorithm with day-by-day context
            val multiDayList =
                dayAssignmentAlgorithm.assignIngredientsToShoppingDaysWithDailyContext(
                    eventId = eventId,
                    eventStartDate = eventStartDate,
                    eventEndDate = HelperFunctions.getLocalDate(event.to),
                    ingredientsPerDay = ingredientsPerDay
                )

            Logger.i("Multi-day calculation completed with ${multiDayList.dailyLists.size} shopping days")

            // Save to repository if requested
            if (saveToRepository) {
                try {
                    eventRepository.saveMultiDayShoppingList(eventId, multiDayList)
                    Logger.i("Multi-day shopping list saved to repository")
                } catch (e: Exception) {
                    Logger.e("Failed to save multi-day shopping list: ${e.message}")
                    // Continue execution - calculation succeeded even if save failed
                }
            }

            return multiDayList

        } catch (e: Exception) {
            Logger.e("Error calculating multi-day shopping list: ${e.message}")
            // Fallback to single-day list
            val legacyList = calculate(eventId)
            val eventStartDate = try {
                val event = eventRepository.getEventById(eventId)
                event?.let { HelperFunctions.getLocalDate(it.from) }
                    ?: HelperFunctions.getCurrentLocalDate()
            } catch (ex: Exception) {
                HelperFunctions.getCurrentLocalDate()
            }

            return MultiDayShoppingList(
                eventId = eventId,
                dailyLists = mapOf(
                    eventStartDate to model.DailyShoppingList(
                        purchaseDate = eventStartDate,
                        ingredients = legacyList
                    )
                )
            )
        }
    }

    /**
     * Calculate ingredients for all meals and aggregate quantities
     */
    private suspend fun calculateIngredientsForAllMeals(
        eventId: String,
        meals: List<model.Meal>,
        existingMap: MutableMap<String, ShoppingIngredient>
    ): Map<String, ShoppingIngredient> {
        // Calculate ingredients needed per day first
        val ingredientsPerDay = calculateIngredientsPerDay(eventId, meals, existingMap)

        // Then aggregate across all days for final totals
        val calculatedShoppingIngredients: MutableMap<String, ShoppingIngredient> =
            existingMap.toMutableMap()

        for ((day, dayIngredients) in ingredientsPerDay) {
            for ((ingredientId, shoppingIngredient) in dayIngredients) {
                val existing = calculatedShoppingIngredients[ingredientId]
                if (existing != null) {
                    // Aggregate amounts
                    existing.amount += shoppingIngredient.amount
                    calculatedShoppingIngredients[ingredientId] = existing
                } else {
                    calculatedShoppingIngredients[ingredientId] = shoppingIngredient
                }
            }
        }

        return calculatedShoppingIngredients
    }

    /**
     * Calculate ingredients needed per day - maintains day-by-day context
     */
    private suspend fun calculateIngredientsPerDay(
        eventId: String,
        meals: List<model.Meal>,
        existingMap: MutableMap<String, ShoppingIngredient>
    ): Map<LocalDate, Map<String, ShoppingIngredient>> {
        val ingredientsPerDay = mutableMapOf<LocalDate, MutableMap<String, ShoppingIngredient>>()

        for (meal in meals) {
            val mealDate = HelperFunctions.getLocalDate(meal.day)
            val dayIngredients = ingredientsPerDay.getOrPut(mealDate) { mutableMapOf() }

            for (recipeSelection in meal.recipeSelections) {
                if (recipeSelection.recipe == null) continue

                val calculatedMap = calculateAmountsForRecipe(
                    existingMap,
                    recipeSelection,
                    eventId = eventId
                ).toMutableMap()

                // Merge into day ingredients
                for ((ingredientId, shoppingIngredient) in calculatedMap) {
                    val existing = dayIngredients[ingredientId]
                    if (existing != null) {
                        existing.amount += shoppingIngredient.amount
                        dayIngredients[ingredientId] = existing
                    } else {
                        dayIngredients[ingredientId] = shoppingIngredient
                    }
                }
            }
        }

        return ingredientsPerDay
    }

    private suspend fun fetchAllParticipantsForEvent(eventId: String): MutableMap<String, Participant> {
        return try {
            val participantTimes =
                eventRepository.getParticipantsOfEvent(eventId, withParticipant = true)
            participantTimes.mapNotNull { participantTime ->
                participantTime.participant?.let { participant ->
                    participant.uid to participant
                }
            }.toMutableStateMap()
        } catch (e: Exception) {
            Logger.e("Error fetching participants for shopping calculation: ${e.message}")
            mutableMapOf()
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
        multiplier: Double? = null,
        eventId: String? = null
    ): Map<String, ShoppingIngredient> {
        if (eventId != null && allParticipants.isEmpty()) {
            allParticipants = fetchAllParticipantsForEvent(eventId)
        }
        val multiplierForIngredients = multiplier ?: getEaterMultiplier(recipeSelection)
        val newIngredientMap = HashMap<String, ShoppingIngredient>()
        for (recipeIngredient in recipeSelection.recipe!!.shoppingIngredients) {
            try {
                Logger.i("Original amount: ${recipeIngredient.amount}")

                val converted = getMetricUnitShoppingIngredient(recipeIngredient)
                val ingredientKey = recipeIngredient.ingredientRef + converted.unit

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
            .sum() + recipeSelection.guestCount
    }

    private suspend fun getMultiplierForParticipant(participantId: String): Double {
        var participant =
            allParticipants[participantId];
        if (participant == null) {
            participant =
                eventRepository.getParticipantById(participantId)
            allParticipants[participantId] = participant!!
        }
        if (participant.birthdate == null) {
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
                if (shopIngredient.nameEnteredByUser != "") shopIngredient.nameEnteredByUser else shopIngredient.ingredientRef + shopIngredient.unit
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

    // Adds existing ingredients from multiday shopping list with amount of 0, to not lose the description
    private fun addExistingIngredientsFromMultiDay(
        multiDayShoppingList: MultiDayShoppingList?
    ): HashMap<String, ShoppingIngredient> {
        val newMap = HashMap<String, ShoppingIngredient>()

        multiDayShoppingList?.dailyLists?.forEach { (_, dailyList) ->
            dailyList.ingredients.forEach { shopIngredient ->
                val ingredientKey =
                    if (shopIngredient.nameEnteredByUser != "") shopIngredient.nameEnteredByUser else shopIngredient.ingredientRef + shopIngredient.unit

                // Only add if not already present (avoid duplicates across days)
                if (!newMap.containsKey(ingredientKey)) {
                    val newShoppingIngredient = ShoppingIngredient().apply {
                        uid = shopIngredient.uid
                        ingredient = shopIngredient.ingredient
                        ingredientRef = shopIngredient.ingredientRef
                        unit = shopIngredient.unit
                        shoppingDone = shopIngredient.shoppingDone
                        nameEnteredByUser = shopIngredient.nameEnteredByUser
                        note = shopIngredient.note
                        amount = 0.0 // Set to 0 to avoid double counting
                    }
                    newMap[ingredientKey] = newShoppingIngredient
                }
            }
        }

        return newMap
    }

}