import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.ShoppingIngredient
import services.shoppingList.CalculateShoppingList
import services.shoppingList.getCategory
import services.shoppingList.groupIngredientByCategory
import services.shoppingList.shoppingDone
import view.event.categorized_shopping_list.EditShoppingListActions
import view.shared.ResultState


data class ShoppingListState(
    val ingredientsByCategory: Map<String, List<ShoppingIngredient>> = emptyMap(),
    val currentList: List<ShoppingIngredient> = emptyList(),
    val eventId: String
)

class CategorizedShoppingListViewModel(
    private val calculateShoppingList: CalculateShoppingList,
    private val eventRepository: EventRepository
) :
    ViewModel() {


    private var _state = MutableStateFlow<ResultState<ShoppingListState>>(ResultState.Loading)
    val state = _state.asStateFlow()

    fun onAction(editShoppingListActions: EditShoppingListActions) {
        when (editShoppingListActions) {
            is EditShoppingListActions.SaveToEvent -> saveListToEvent()
            is EditShoppingListActions.ToggleShoppingDone -> toggleShoppingDone(
                editShoppingListActions.shoppingIngredient
            )
        }
    }

    fun initializeShoppingList(eventId: String) {
        _state.value = ResultState.Loading
        viewModelScope.launch {
            val ingredientsList = calculateShoppingList.calculate(eventId)

            val ingredientsByCategory = groupIngredientByCategory(ingredientsList)
            val list = ingredientsByCategory.toList()
            val (done, notDone) = list.partition { it.first == shoppingDone }
            val sortedList = notDone + done;
            val sortedMap = sortedList.toMap(LinkedHashMap())
            _state.value = ResultState.Success(
                ShoppingListState(
                    eventId = eventId,
                    ingredientsByCategory = sortedMap,
                    currentList = ingredientsList
                )
            )
        }
    }

    private fun saveListToEvent() {
        val successData = state.value.getSuccessData() ?: return
        viewModelScope.launch {
            eventRepository.saveShoppingList(successData.eventId, successData.currentList)
        }
    }

    fun toggleShoppingDone(ingredient: ShoppingIngredient) {
        // Remove from old List
        val sucessData = state.value.getSuccessData() ?: return
        val oldCategory = getCategory(ingredient);
        ingredient.shoppingDone = !ingredient.shoppingDone
        val listToRemove = sucessData.ingredientsByCategory[oldCategory]?.toMutableList()
        listToRemove?.remove(ingredient)

        // Add to new List
        val newCategory = getCategory(ingredient);
        val listToAdd =
            sucessData.ingredientsByCategory[newCategory]?.toMutableList() ?: mutableListOf()
        listToAdd.add(ingredient)

        // Update State
        val newIngredientByCategory = sucessData.ingredientsByCategory.toMutableMap();
        newIngredientByCategory[oldCategory] = listToRemove!!
        newIngredientByCategory[newCategory] = listToAdd
        _state.value = ResultState.Success(
            sucessData.copy(
                ingredientsByCategory = newIngredientByCategory
            )
        )
    }
}

