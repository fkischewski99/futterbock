package view.event.recepie_overview_screen

import model.RecipeSelection
import view.event.actions.BaseAction
import view.event.actions.EditEventActions

interface RecipeOverviewActions : BaseAction {
    data class InitializeScreen(val recipeSelection: RecipeSelection, val eventId: String?) :
        RecipeOverviewActions

    data class UpdateNumberOfPortions(val newNumberOfPortions: Int) : RecipeOverviewActions
}