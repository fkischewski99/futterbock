package view.event.categorized_shopping_list

import model.ShoppingIngredient
import view.event.actions.BaseAction

interface EditShoppingListActions : BaseAction {
    data class ToggleShoppingDone(val shoppingIngredient: ShoppingIngredient) :
        EditShoppingListActions

    data object SaveToEvent : EditShoppingListActions
    data class Initialize(val eventId: String) : EditShoppingListActions
}