import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.ShoppingIngredient
import services.materiallist.CalculateMaterialList
import services.shoppingList.CalculateShoppingList
import services.shoppingList.getCategory
import services.shoppingList.groupIngredientByCategory
import services.shoppingList.shoppingDone
import view.event.categorized_shopping_list.EditShoppingListActions
import view.event.materiallist.EditMaterialListActions
import view.shared.ResultState


data class MaterialListState(
    val materialList: Map<String, Int> = emptyMap(),
    val eventId: String
)

class MaterialListViewModel(
    private val calculateMaterialList: CalculateMaterialList,
) :
    ViewModel() {


    private var _state = MutableStateFlow<ResultState<MaterialListState>>(ResultState.Loading)
    val state = _state.asStateFlow()

    fun onAction(materialListActions: EditMaterialListActions) {
        when (materialListActions) {
            is EditMaterialListActions.Initialize -> {
                initializeShoppingList(materialListActions.eventId)
            }
        }
    }

    private fun initializeShoppingList(eventId: String) {
        _state.value = ResultState.Loading
        viewModelScope.launch {
            val materialList = calculateMaterialList.calculate(eventId)

            _state.value = ResultState.Success(
                MaterialListState(
                    eventId = eventId,
                    materialList = materialList
                )
            )
        }
    }
}

