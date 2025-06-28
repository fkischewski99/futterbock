package services.shoppingList

import model.ShoppingIngredient

const val shoppingDone = "Bereits im Einkaufswagen";

fun groupIngredientByCategory(ingredientsList: List<ShoppingIngredient>) =
    ingredientsList.groupBy {
        getCategory(it)
    }.mapValues { (_, ingredients) ->
        ingredients.sortedBy { it.ingredient?.name ?: it.nameEnteredByUser }
    }

fun getCategory(it: ShoppingIngredient): String {
    if (it.shoppingDone) {
        return shoppingDone
    } else if (it.nameEnteredByUser != "") {
        return "Manuell hinzugefügt"
    } else {
        return it.ingredient?.category ?: "Andere"
    }
}