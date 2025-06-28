import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import model.MultiDayShoppingList
import model.ShoppingIngredient
import model.Source
import services.shoppingList.CalculateShoppingList
import services.shoppingList.getCategory
import services.shoppingList.groupIngredientByCategory
import services.shoppingList.shoppingDone
import view.event.categorized_shopping_list.EditShoppingListActions
import view.shared.HelperFunctions
import view.shared.ResultState


data class ShoppingListState(
    val ingredientsByCategory: Map<String, List<ShoppingIngredient>> = emptyMap(),
    val currentList: List<ShoppingIngredient> = emptyList(),
    val eventId: String,
    val multiDayShoppingList: MultiDayShoppingList? = null,
    val selectedDate: LocalDate? = null,
)

class CategorizedShoppingListViewModel(
    private val calculateShoppingList: CalculateShoppingList,
    private val eventRepository: EventRepository
) :
    ViewModel() {


    private var _state = MutableStateFlow<ResultState<ShoppingListState>>(ResultState.Loading)
    val state = _state.asStateFlow()

    fun onAction(editShoppingListActions: EditShoppingListActions) {
        try {


            when (editShoppingListActions) {
                is EditShoppingListActions.SaveToEvent -> saveListToEvent()
                is EditShoppingListActions.ToggleShoppingDone -> toggleShoppingDone(
                    editShoppingListActions.shoppingIngredient
                )

                is EditShoppingListActions.Initialize -> initializeShoppingList(
                    editShoppingListActions.eventId
                )

                is EditShoppingListActions.AddNewIngredient -> addIngredientToList(
                    editShoppingListActions.ingredient
                )

                is EditShoppingListActions.DeleteShoppingItem -> deleteIngredient(
                    editShoppingListActions.shoppingIngredient
                )

                is EditShoppingListActions.InitializeMultiDay -> initializeMultiDayShoppingList(
                    editShoppingListActions.eventId
                )

                is EditShoppingListActions.SelectShoppingDay -> selectShoppingDay(
                    editShoppingListActions.date
                )

                is EditShoppingListActions.ToggleMultiDayMode -> toggleMultiDayMode()
            }
        } catch (e: Exception) {
            _state.value = ResultState.Error("Fehler beim laden der Einkaufsliste")
        }
    }

    private fun deleteIngredient(shoppingIngredient: ShoppingIngredient) {
        val successData = state.value.getSuccessData() ?: return

        viewModelScope.launch {
            // Delete from multiday shopping list - update the specific day
            val updatedDailyLists = successData.multiDayShoppingList!!.dailyLists.toMutableMap()
            val currentDayList = updatedDailyLists[successData.selectedDate!!]!!
            val updatedIngredients = currentDayList.ingredients.toMutableList()
            updatedIngredients.remove(shoppingIngredient)
            updatedDailyLists[successData.selectedDate] = currentDayList.copy(
                ingredients = updatedIngredients
            )
            val updatedMultiDayList = successData.multiDayShoppingList.copy(
                dailyLists = updatedDailyLists
            )
            eventRepository.saveMultiDayShoppingList(successData.eventId, updatedMultiDayList)
            
            _state.value = ResultState.Success(
                successData.copy(
                    currentList = updatedIngredients,
                    ingredientsByCategory = groupIngredientByCategory(updatedIngredients),
                    multiDayShoppingList = updatedMultiDayList
                )
            )
        }

    }

    private fun initializeShoppingList(eventId: String) {
        // Always use multi-day mode now
        initializeMultiDayShoppingList(eventId)
    }

    /**
     * Initialize multi-day shopping list mode
     */
    private fun initializeMultiDayShoppingList(eventId: String) {
        _state.value = ResultState.Loading
        viewModelScope.launch {
            try {
                // First try to load existing multi-day shopping list
                var multiDayList = eventRepository.getMultiDayShoppingList(eventId)

                // If none exists, calculate a new one
                multiDayList = calculateShoppingList.calculateMultiDay(eventId)


                // Get the first shopping day or current day
                val firstShoppingDay = multiDayList.getShoppingDaysInOrder().firstOrNull()
                val dailyList = firstShoppingDay?.let { multiDayList.dailyLists[it] }

                val ingredientsByCategory = if (dailyList != null) {
                    groupIngredientByCategory(dailyList.ingredients)
                } else {
                    emptyMap()
                }

                val list = ingredientsByCategory.toList()
                val (done, notDone) = list.partition { it.first == shoppingDone }
                val sortedList = notDone + done
                val sortedMap = sortedList.toMap(LinkedHashMap())

                _state.value = ResultState.Success(
                    ShoppingListState(
                        eventId = eventId,
                        ingredientsByCategory = sortedMap,
                        currentList = dailyList?.ingredients ?: emptyList(),
                        multiDayShoppingList = multiDayList,
                        selectedDate = firstShoppingDay,
                    )
                )
            } catch (e: Exception) {
                _state.value =
                    ResultState.Error("Fehler beim Laden der mehrtägigen Einkaufsliste: ${e.message}")
            }
        }
    }

    /**
     * Switch to a different shopping day
     */
    private fun selectShoppingDay(date: LocalDate) {
        val currentState = state.value.getSuccessData() ?: return
        val multiDayList = currentState.multiDayShoppingList ?: return

        val dailyList = multiDayList.dailyLists[date]
        if (dailyList == null) {
            _state.value = ResultState.Error("Einkaufsliste für den gewählten Tag nicht gefunden")
            return
        }

        val ingredientsByCategory = groupIngredientByCategory(dailyList.ingredients)
        val list = ingredientsByCategory.toList()
        val (done, notDone) = list.partition { it.first == shoppingDone }
        val sortedList = notDone + done
        val sortedMap = sortedList.toMap(LinkedHashMap())

        _state.value = ResultState.Success(
            currentState.copy(
                ingredientsByCategory = sortedMap,
                currentList = dailyList.ingredients,
                selectedDate = date
            )
        )
    }

    /**
     * Toggle between single-day and multi-day mode (disabled - always multi-day now)
     */
    fun toggleMultiDayMode() {
        // No longer needed - always in multi-day mode
    }

    private fun addIngredientToList(ingredient: String) {
        val successData = state.value.getSuccessData() ?: return
        val list = successData.currentList.toMutableList()
        val shoppingIngredient = ShoppingIngredient()
        shoppingIngredient.nameEnteredByUser = ingredient
        shoppingIngredient.uid = HelperFunctions.generateRandomStringId(20)
        shoppingIngredient.source = Source.ENTERED_BY_USER

        list.add(shoppingIngredient)
        _state.value = ResultState.Success(
            successData.copy(
                currentList = list,
                ingredientsByCategory = groupIngredientByCategory(list)
            )
        )
    }

    private fun saveListToEvent() {
        val successData = state.value.getSuccessData() ?: return
        viewModelScope.launch {
            if (successData.multiDayShoppingList != null && successData.selectedDate != null) {
                // Save multi-day shopping list - update the specific day
                val updatedDailyLists = successData.multiDayShoppingList.dailyLists.toMutableMap()
                updatedDailyLists[successData.selectedDate] = model.DailyShoppingList(
                    purchaseDate = successData.selectedDate,
                    ingredients = successData.currentList
                )
                val updatedMultiDayList = successData.multiDayShoppingList.copy(
                    dailyLists = updatedDailyLists
                )
                eventRepository.saveMultiDayShoppingList(successData.eventId, updatedMultiDayList)
            }
        }
    }

    private fun toggleShoppingDone(ingredient: ShoppingIngredient) {
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

