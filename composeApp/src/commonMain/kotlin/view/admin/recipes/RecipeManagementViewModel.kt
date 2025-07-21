package view.admin.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Recipe
import view.shared.ResultState
import view.shared.HelperFunctions.Companion.generateRandomStringId
import view.event.new_meal_screen.RecipeViewModel

class RecipeManagementViewModel(
    private val eventRepository: EventRepository,
    private val recipeViewModel: RecipeViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultState<List<Recipe>>>(ResultState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        loadRecipes()
    }

    fun onAction(action: RecipeManagementAction) {
        when (action) {
            is RecipeManagementAction.LoadRecipes -> loadRecipes()
            is RecipeManagementAction.CreateRecipe -> createRecipe(action.recipe)
            is RecipeManagementAction.UpdateRecipe -> updateRecipe(action.recipe)
            is RecipeManagementAction.DeleteRecipe -> deleteRecipe(action.recipe)
        }
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            try {
                _uiState.value = ResultState.Loading
                // Load only user-created recipes from the user's group
                val recipes = eventRepository.getUserCreatedRecipes()
                _uiState.value = ResultState.Success(recipes)
            } catch (e: Exception) {
                _uiState.value = ResultState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }

    private fun createRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                // The repository will handle setting the group and source when creating
                val recipeWithId = recipe.apply {
                    uid = generateRecipeId()
                }
                
                eventRepository.createRecipe(recipeWithId)
                _message.value = "Rezept erfolgreich erstellt"
                loadRecipes() // Refresh the management list
                recipeViewModel.onAction(RecipeManagementAction.CreateRecipe(recipeWithId)) // Add to the main recipe list
            } catch (e: Exception) {
                _message.value = "Fehler beim Erstellen des Rezepts: ${e.message}"
            }
        }
    }

    private fun updateRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                eventRepository.updateRecipe(recipe)
                _message.value = "Rezept erfolgreich aktualisiert"
                loadRecipes() // Refresh the management list
                recipeViewModel.onAction(RecipeManagementAction.UpdateRecipe(recipe)) // Update in the main recipe list
            } catch (e: Exception) {
                _message.value = "Fehler beim Aktualisieren des Rezepts: ${e.message}"
            }
        }
    }

    private fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                eventRepository.deleteRecipe(recipe.uid)
                _message.value = "Rezept erfolgreich gelöscht"
                loadRecipes() // Refresh the management list
                recipeViewModel.onAction(RecipeManagementAction.DeleteRecipe(recipe)) // Remove from the main recipe list
            } catch (e: Exception) {
                _message.value = "Fehler beim Löschen des Rezepts: ${e.message}"
            }
        }
    }

    private fun generateRecipeId(): String {
        return generateRandomStringId()
    }

    fun clearMessage() {
        _message.value = null
    }
}