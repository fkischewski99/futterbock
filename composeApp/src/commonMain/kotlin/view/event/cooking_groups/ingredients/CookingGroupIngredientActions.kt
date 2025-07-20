package view.event.cooking_groups.ingredients

import model.ParticipantTime
import view.event.actions.BaseAction

sealed interface CookingGroupIngredientActions : BaseAction {
    data object Initilize : CookingGroupIngredientActions
    data object Refresh : CookingGroupIngredientActions
    data object NextDay : CookingGroupIngredientActions
    data object PreviousDay : CookingGroupIngredientActions

}