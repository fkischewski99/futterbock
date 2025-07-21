package view.admin.recipes

import model.Recipe

sealed interface RecipeManagementAction {
    data object LoadRecipes : RecipeManagementAction
    data class CreateRecipe(val recipe: Recipe) : RecipeManagementAction
    data class UpdateRecipe(val recipe: Recipe) : RecipeManagementAction
    data class DeleteRecipe(val recipe: Recipe) : RecipeManagementAction
}