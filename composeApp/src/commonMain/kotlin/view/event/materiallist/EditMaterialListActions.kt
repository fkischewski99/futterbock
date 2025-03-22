package view.event.materiallist

import model.Material
import model.ShoppingIngredient
import view.event.actions.BaseAction

interface EditMaterialListActions : BaseAction {
    data class Initialize(val eventId: String) : EditMaterialListActions
    data class Add(val materialName: String) : EditMaterialListActions
    data class Delete(val material: Material) : EditMaterialListActions
    data object SaveMaterialList : EditMaterialListActions
}