package view.event.new_meal_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Recipe
import view.admin.recipes.RecipeManagementAction

class RecipeViewModel(val eventRepository: EventRepository) : ViewModel() {

    private var _state = MutableStateFlow<List<Recipe>>(listOf())
    val state = _state.asStateFlow()

    init {
        onAction(RecipeManagementAction.LoadRecipes)
    }
    
    fun onAction(action: RecipeManagementAction) {
        when (action) {
            is RecipeManagementAction.LoadRecipes -> loadRecipes()
            is RecipeManagementAction.CreateRecipe -> addRecipe(action.recipe)
            is RecipeManagementAction.UpdateRecipe -> updateRecipe(action.recipe)
            is RecipeManagementAction.DeleteRecipe -> deleteRecipe(action.recipe.uid)
        }
    }
    
    private fun loadRecipes() {
        viewModelScope.launch {
            Logger.i("Request all recipes")
            _state.value = eventRepository.getAllRecipes()
        }
    }
    
    private fun addRecipe(recipe: Recipe) {
        val currentRecipes = _state.value.toMutableList()
        currentRecipes.add(recipe)
        _state.value = currentRecipes
        Logger.i("Added recipe to list: ${recipe.name}")
    }
    
    private fun updateRecipe(updatedRecipe: Recipe) {
        val currentRecipes = _state.value.toMutableList()
        val index = currentRecipes.indexOfFirst { it.uid == updatedRecipe.uid }
        if (index != -1) {
            currentRecipes[index] = updatedRecipe
            _state.value = currentRecipes
            Logger.i("Updated recipe in list: ${updatedRecipe.name}")
        }
    }
    
    private fun deleteRecipe(recipeId: String) {
        val currentRecipes = _state.value.toMutableList()
        currentRecipes.removeAll { it.uid == recipeId }
        _state.value = currentRecipes
        Logger.i("Removed recipe from list: $recipeId")
    }

}