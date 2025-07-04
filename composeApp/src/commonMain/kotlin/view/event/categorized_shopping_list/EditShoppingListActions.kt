package view.event.categorized_shopping_list

import kotlinx.datetime.LocalDate
import model.ShoppingIngredient
import view.event.actions.BaseAction

interface EditShoppingListActions : BaseAction {
    data class ToggleShoppingDone(val shoppingIngredient: ShoppingIngredient) :
        EditShoppingListActions

    data object SaveToEvent : EditShoppingListActions
    data class AddNewIngredient(val ingredient: String) : EditShoppingListActions
    data class Initialize(val eventId: String) : EditShoppingListActions
    data class InitializeMultiDay(val eventId: String) : EditShoppingListActions
    data class SelectShoppingDay(val date: LocalDate) : EditShoppingListActions
    class DeleteShoppingItem(val shoppingIngredient: ShoppingIngredient) : EditShoppingListActions {

    }
}