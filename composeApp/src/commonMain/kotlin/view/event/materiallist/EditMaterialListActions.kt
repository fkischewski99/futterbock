package view.event.materiallist

import model.ShoppingIngredient
import view.event.actions.BaseAction

interface EditMaterialListActions : BaseAction {
    data class Initialize(val eventId: String) : EditMaterialListActions
}