package services.shoppingList

import model.ShoppingIngredient

const val shoppingDone = "Bereits im Einkaufswagen";

fun groupIngredientByCategory(ingredientsList: List<ShoppingIngredient>) =
    ingredientsList.groupBy {
        getCategory(it)
    }

fun getCategory(it: ShoppingIngredient): String {
    if (it.shoppingDone) {
        return shoppingDone
    } else {
        return it.ingredient?.category ?: "Andere"
    }
}