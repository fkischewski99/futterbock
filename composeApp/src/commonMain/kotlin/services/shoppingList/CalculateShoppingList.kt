package services.shoppingList

import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import model.IngredientUnit
import model.RecipeSelection
import model.ShoppingIngredient
import kotlin.coroutines.coroutineContext

//Calculates the Amount of Ingredients that need to be shoppend
class CalculateShoppingList(private val eventRepository: EventRepository) {

    suspend fun calculate(eventId: String): List<ShoppingIngredient> {
        val shoppingIngredients = eventRepository.getShoppingIngredients(eventId)
        val map: MutableMap<String, ShoppingIngredient> = HashMap()
        addExistingIngredients(shoppingIngredients, map)
        addAllAmounts(eventId, map)
        return map.values.toList().sortedBy { it.ingredient?.name }
    }


    private suspend fun addAllAmounts(
        eventId: String,
        map: MutableMap<String, ShoppingIngredient>
    ) {
        val listOfMeals = eventRepository.getMealsWithRecipeAndIngredients(eventId)


        for (meal in listOfMeals) {
            for (recipeSelection in meal.recipeSelections) {
                if (null != recipeSelection.recipe) {
                    calculateAmountsForRecipe(map, recipeSelection)
                }
            }
        }
    }

    /**
     * Calculates the amount of ingredients for a recipe
     * If multiplier is added it is used, else the size of eater ids is used
     */
    fun calculateAmountsForRecipe(
        map: MutableMap<String, ShoppingIngredient>,
        recipeSelection: RecipeSelection,
        multiplier: Int? = null
    ): Map<String, ShoppingIngredient> {
        for (recipeIngredient in recipeSelection.recipe!!.shoppingIngredients) {
            try {
                Logger.i("recipe ${recipeIngredient.amount}")
                val ingredient = getMetricUnitShoppingIngredient(
                    recipeIngredient,
                )
                Logger.i("recipe ${recipeIngredient.amount}")
                val shoppingIngredient: ShoppingIngredient =
                    map[recipeIngredient.ingredientRef] ?: ingredient.apply { amount = 0.0 }

                if (shoppingIngredient.ingredient == null) shoppingIngredient.ingredient =
                    recipeIngredient.ingredient

                ingredient.note = shoppingIngredient.note
                if (ingredient.unit == shoppingIngredient.unit) {
                    val multiplierForIngredients = multiplier ?: getEaterMultiplier(recipeSelection)
                    val amountToAdd = recipeIngredient.amount * multiplierForIngredients;
                    Logger.i("amounttoadd: $amountToAdd, multiplier: $multiplierForIngredients, recipeIngredient: ${recipeIngredient.amount}")
                    ingredient.amount += amountToAdd
                    map[recipeIngredient.ingredientRef] = ingredient
                }
            } catch (e: Exception) {
                println(
                    "Error Calculation Amount for " + (recipeIngredient.ingredient
                        ?.name ?: "")
                )
                println(e.printStackTrace())
            }
        }
        return map
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

    private fun getEaterMultiplier(recipeSelection: RecipeSelection): Int {
        return recipeSelection.eaterIds.size
    }

    // Adds existing ingredients with a amount of 0, to not lose the description
    private fun addExistingIngredients(
        listOfShoppingIngredients: List<ShoppingIngredient>,
        map: MutableMap<String, ShoppingIngredient>
    ) {
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
            }
            shopIngredient.amount = 0.0
            map[ingredientKey] = newShoppingIngredient
        }
    }
}