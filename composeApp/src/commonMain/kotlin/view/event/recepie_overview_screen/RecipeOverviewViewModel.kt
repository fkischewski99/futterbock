package view.event.recepie_overview_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
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
    val calculatedIngredientAmounts: List<ShoppingIngredient>,
    val numberOfPortions: Int = 1
)

class RecipeOverviewViewModel(
    private val eventRepository: EventRepository,
    private val calculatedIngredientAmounts: CalculateShoppingList
) : ViewModel() {
    private var _recipeState =
        MutableStateFlow<ResultState<RecipeOverviewState>>(ResultState.Loading)
    val recipeState = _recipeState.asStateFlow()

    private fun initializeViewModel(recipeSelection: RecipeSelection) {
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
                    calculatedIngredientAmounts = calulatedMap.values.toList(),
                    numberOfPortions = recipeSelection.eaterIds.size
                )
            )
        }
    }

    private fun changePortionNumber(numberOfPortions: Int) {
        val state = _recipeState.value.getSuccessData() ?: return
        viewModelScope.launch {
            val calulatedMap = calculatedIngredientAmounts.calculateAmountsForRecipe(
                existingShoppingIngredients = mutableMapOf(),
                recipeSelection = state.recipeSelection,
                multiplier = numberOfPortions.toDouble()

            )
            Logger.i("Update number of portions to $numberOfPortions")

            _recipeState.value = ResultState.Success(
                state.copy(calculatedIngredientAmounts = calulatedMap.values.toList())
            )
        }
    }

    fun handleAction(action: RecipeOverviewActions) {
        when (action) {
            is RecipeOverviewActions.InitializeScreen -> initializeViewModel(action.recipeSelection)
            is RecipeOverviewActions.UpdateNumberOfPortions -> changePortionNumber(action.newNumberOfPortions)
        }
    }
}