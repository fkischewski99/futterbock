package view.event.recepie_overview_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.RecipeSelection
import model.ShoppingIngredient
import services.shoppingList.CalculateShoppingList
import view.shared.ResultState

data class RecipeOverviewState(
    val recipeSelection: RecipeSelection,
    val calculatedIngredientAmounts: List<ShoppingIngredient>
)

class RecipeOverviewViewModel(
    private val eventRepository: EventRepository,
    private val calculatedIngredientAmounts: CalculateShoppingList
) : ViewModel() {
    private var _recipeState =
        MutableStateFlow<ResultState<RecipeOverviewState>>(ResultState.Loading)
    val recipeState = _recipeState.asStateFlow()

    fun initializeViewModel(recipeSelection: RecipeSelection) {
        _recipeState.value = ResultState.Loading
        viewModelScope.launch {
            recipeSelection.recipe = eventRepository.getRecipeById(recipeSelection.recipeRef)
            val calulatedMap = calculatedIngredientAmounts.calculateAmountsForRecipe(
                mutableMapOf(),
                recipeSelection
            )
            _recipeState.value = ResultState.Success(
                RecipeOverviewState(
                    recipeSelection = recipeSelection,
                    calculatedIngredientAmounts = calulatedMap.values.toList()
                )
            )
        }
    }
}