package services.shoppingList

import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import model.DailyShoppingList
import model.MultiDayShoppingList
import model.ShoppingIngredient

/**
 * Algorithm for assigning ingredients to optimal purchase days based on expiration dates
 */
class DayAssignmentAlgorithm {

    /**
     * Enhanced version that uses deferred list approach to minimize shopping trips
     *
     * Algorithm logic:
     * 1. Classify ingredients: First day (no expiration issues) vs Deferred (expiration issues)
     * 2. For deferred ingredients, use trip-minimizing strategy:
     *    a) Find ingredient that needs to be bought FIRST (earliest: day of cooking - expirationDays)
     *    b) Group other ingredients that can be bought on the same day
     *    c) Criteria for grouping: day of cooking - expirationDays <= shopping day AND day of cooking > shopping day
     * 3. Repeat until all deferred ingredients are assigned
     *
     * Example: Lettuce expires in 3 days, used on day 4 -> must buy by day 1
     *         Tomatoes expire in 2 days, used on day 5 -> must buy by day 3
     *         Bread expires in 1 day, used on day 6 -> must buy by day 5
     * -> Shopping trips: Day 1 (lettuce + anything else valid), Day 3 (tomatoes), Day 5 (bread)
     */
    fun assignIngredientsToShoppingDaysWithDailyContext(
        eventId: String,
        eventStartDate: LocalDate,
        eventEndDate: LocalDate,
        ingredientsPerDay: Map<LocalDate, Map<String, ShoppingIngredient>>
    ): MultiDayShoppingList {
        Logger.i("Starting enhanced day assignment with daily context for event $eventId")


        // For each ingredient, decide optimal purchase strategy
        val purchaseStrategy = decidePurchaseStrategy(
            ingredientsPerDay = ingredientsPerDay,
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate,
        )

        // Build daily shopping lists based on purchase strategy
        val dailyLists = buildDailyListsFromStrategy(purchaseStrategy)

        Logger.i("Enhanced day assignment completed. Created ${dailyLists.size} shopping days")
        return MultiDayShoppingList(eventId, dailyLists)
    }

    /**
     * Decide purchase strategy using deferred list approach to minimize shopping trips
     */
    private fun decidePurchaseStrategy(
        ingredientsPerDay: Map<LocalDate, Map<String, ShoppingIngredient>>,
        eventStartDate: LocalDate,
        eventEndDate: LocalDate,
    ): Map<LocalDate, MutableList<ShoppingIngredient>> {

        // Step 1: Initial classification
        val (firstDayIngredients, deferredIngredients) = classifyIngredients(
            ingredientsPerDay = ingredientsPerDay,
            eventStartDate = eventStartDate,
            eventEndDate = eventEndDate
        )

        // Step 2: Process deferred ingredients to minimize shopping trips
        return processDeferredIngredientsForMinimalTrips(
            firstDayIngredients, deferredIngredients, eventStartDate, eventEndDate
        )
    }

    /**
     * Classify ingredients into first day (no expiration issues) and deferred (expiration issues)
     */
    private fun classifyIngredients(
        ingredientsPerDay: Map<LocalDate, Map<String, ShoppingIngredient>>,
        eventStartDate: LocalDate,
        eventEndDate: LocalDate
    ): Pair<MutableList<ShoppingIngredient>, Map<LocalDate, MutableList<ShoppingIngredient>>> {

        val firstDayIngredients = mutableListOf<ShoppingIngredient>()
        val deferredIngredients = mutableMapOf<LocalDate, MutableList<ShoppingIngredient>>()

        // Build total ingredients map from daily ingredients
        val durationOfEvent = daysBetween(startDate = eventStartDate, endDate = eventEndDate)
        for ((day, dayIngredients) in ingredientsPerDay) {
            for ((ingredientId, shoppingIngredient) in dayIngredients) {
                val expirationOfIngredient =
                    shoppingIngredient.ingredient?.expirationDateInDays ?: 999
                val daysBetweenStartAndCookingDay =
                    daysBetween(startDate = eventStartDate, endDate = day)
                if (expirationOfIngredient > durationOfEvent || daysBetweenStartAndCookingDay <= expirationOfIngredient) {
                    firstDayIngredients.add(shoppingIngredient)
                } else {
                    Logger.i("Ingredient ${shoppingIngredient.ingredient?.name} expires in $expirationOfIngredient days and got deferred. Is needed on day: $day")
                    deferredIngredients.getOrPut(day) { mutableListOf() }.add(shoppingIngredient)
                }
            }

        }
        // return classification
        return firstDayIngredients to deferredIngredients
    }

    /**
     * Process deferred ingredients to minimize shopping trips
     * Enhanced to handle ingredient splitting when expiration doesn't cover all usage days
     */
    private fun processDeferredIngredientsForMinimalTrips(
        firstDayIngredients: List<ShoppingIngredient>,
        deferredIngredients: Map<LocalDate, List<ShoppingIngredient>>,
        eventStartDate: LocalDate,
        eventEndDate: LocalDate
    ): Map<LocalDate, MutableList<ShoppingIngredient>> {

        val purchaseStrategy = mutableMapOf<LocalDate, MutableList<ShoppingIngredient>>()

        // Add first day ingredients
        if (firstDayIngredients.isNotEmpty()) {
            val purchaseDateIngredients = mutableMapOf<String, ShoppingIngredient>()
            firstDayIngredients.forEach {
                putIngredientIntoPurchaseDate(it, purchaseDateIngredients)
            }
            purchaseStrategy[eventStartDate] = purchaseDateIngredients.values.toMutableList()
        }

        var minDate = findLatestRequiredPurchaseDate(deferredIngredients, eventEndDate);
        var newDeferredIngredients = deferredIngredients;
        val durationOfEvent = daysBetween(startDate = eventStartDate, endDate = eventEndDate)
        var numberOfRuns = 0;
        while (newDeferredIngredients.values.isNotEmpty()) {
            newDeferredIngredients =
                addAllIngredientsThatCanBuyOnDate(minDate, newDeferredIngredients, purchaseStrategy)
            minDate = findLatestRequiredPurchaseDate(newDeferredIngredients, eventEndDate);
            numberOfRuns++;
            if (numberOfRuns > durationOfEvent) {
                break;
            }
        }


        return purchaseStrategy
    }

    private fun addAllIngredientsThatCanBuyOnDate(
        purchaseDate: LocalDate,
        deferredIngredients: Map<LocalDate, List<ShoppingIngredient>>,
        purchaseStrategy: MutableMap<LocalDate, MutableList<ShoppingIngredient>>
    ): MutableMap<LocalDate, MutableList<ShoppingIngredient>> {
        val purchaseDateIngredients = mutableMapOf<String, ShoppingIngredient>()
        val newDeferredIngredients = mutableMapOf<LocalDate, MutableList<ShoppingIngredient>>()
        for ((date, ingredients) in deferredIngredients) {
            ingredients.forEach { ingredient ->
                val latestPurchaseDate =
                    date.minus(kotlinx.datetime.DatePeriod(days = ingredient.ingredient!!.expirationDateInDays!!))
                if (latestPurchaseDate < purchaseDate) {
                    putIngredientIntoPurchaseDate(ingredient, purchaseDateIngredients)
                } else {
                    newDeferredIngredients.getOrPut(date) { mutableListOf() }.add(ingredient)
                }
            }
        }
        purchaseStrategy[purchaseDate] = purchaseDateIngredients.values.toMutableList()
        return newDeferredIngredients;
    }

    private fun putIngredientIntoPurchaseDate(
        ingredient: ShoppingIngredient,
        purchaseDateIngredients: MutableMap<String, ShoppingIngredient>
    ) {
        val ingredientKey = ingredient.ingredientRef + ingredient.unit
        val existingIngredient = purchaseDateIngredients[ingredientKey]
        if (existingIngredient != null) {
            existingIngredient.amount += ingredient.amount
        } else {
            purchaseDateIngredients[ingredientKey] = ingredient
        }
    }

    /**
     * Find ingredient with earliest required purchase date (must be bought first)
     */
    private fun findLatestRequiredPurchaseDate(
        deferredIngredients: Map<LocalDate, List<ShoppingIngredient>>,
        endDate: LocalDate
    ): LocalDate {
        var minDate = endDate;
        for ((date, ingredients) in deferredIngredients) {
            if (date < minDate) {
                minDate = date
            }
        }
        return minDate;
    }


    /**
     * Build daily shopping lists from purchase strategy
     */
    private fun buildDailyListsFromStrategy(
        purchaseStrategy: Map<LocalDate, List<ShoppingIngredient>>
    ): Map<LocalDate, DailyShoppingList> {
        return purchaseStrategy.mapValues { (date, ingredients) ->
            DailyShoppingList(
                purchaseDate = date,
                ingredients = ingredients
            )
        }
    }

    /**
     * Calculate days between two dates
     */
    private fun daysBetween(startDate: LocalDate, endDate: LocalDate): Int {
        return (endDate.toEpochDays() - startDate.toEpochDays()).toInt()
    }
}