import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Material
import model.ShoppingIngredient
import model.Source
import services.materiallist.CalculateMaterialList
import services.shoppingList.CalculateShoppingList
import services.shoppingList.getCategory
import services.shoppingList.groupIngredientByCategory
import services.shoppingList.shoppingDone
import view.event.categorized_shopping_list.EditShoppingListActions
import view.event.materiallist.EditMaterialListActions
import view.shared.HelperFunctions
import view.shared.ResultState


data class MaterialListState(
    val materialList: List<Material> = emptyList(),
    val allMaterialList: List<Material> = emptyList(),
    val eventId: String
)

class MaterialListViewModel(
    private val calculateMaterialList: CalculateMaterialList,
    private val eventRepository: EventRepository
) :
    ViewModel() {


    private var _state = MutableStateFlow<ResultState<MaterialListState>>(ResultState.Loading)
    val state = _state.asStateFlow()

    fun onAction(materialListActions: EditMaterialListActions) {
        try {


            when (materialListActions) {
                is EditMaterialListActions.Initialize -> {
                    initializeShoppingList(materialListActions.eventId)
                }

                is EditMaterialListActions.Add -> {
                    addMaterialList(materialListActions.materialName)
                }

                is EditMaterialListActions.SaveMaterialList -> {
                    saveMaterialList()
                }

                is EditMaterialListActions.Delete -> {
                    deleteMaterial(materialListActions.materialId)
                }
            }
        } catch (e: Exception) {
            _state.value = ResultState.Error("Fehler beim Laden der Materialliste")
        }
    }

    private fun deleteMaterial(materialIdToDelete: String) {
        var state = _state.value.getSuccessData() ?: return
        val materialList = state.materialList.toMutableList()

        val materialToDelete = materialList.firstOrNull { it.uid == materialIdToDelete }
        if (materialToDelete == null) {
            return
        }
        materialList.remove(materialToDelete)
        viewModelScope.launch {
            _state.value = ResultState.Success(
                MaterialListState(
                    eventId = state.eventId,
                    materialList = materialList
                )
            )
            eventRepository.deleteMaterialById(state.eventId, materialIdToDelete)
        }

    }

    private fun saveMaterialList() {
        var state = _state.value.getSuccessData() ?: return
        viewModelScope.launch {
            eventRepository.saveMaterialList(
                state.eventId,
                state.materialList.filter { it.source == Source.ENTERED_BY_USER })
        }
    }

    private fun initializeShoppingList(eventId: String) {
        viewModelScope.launch {
            val allMaterials =
                if (state.value is ResultState.Success) state.value.getSuccessData()!!.allMaterialList else eventRepository.getAllMaterials()

            _state.value = ResultState.Loading

            val materialList = calculateMaterialList.calculate(eventId)

            _state.value = ResultState.Success(
                MaterialListState(
                    eventId = eventId,
                    materialList = materialList,
                    allMaterialList = allMaterials
                )
            )
        }
    }

    private fun addMaterialList(materialName: String) {
        var state = _state.value.getSuccessData() ?: return
        val materialList = state.materialList.toMutableList()
        materialList.add(Material().apply {
            uid = HelperFunctions.generateRandomStringId(20)
            name = materialName
            source = Source.ENTERED_BY_USER
        })
        viewModelScope.launch {
            _state.value = ResultState.Success(
                MaterialListState(
                    eventId = state.eventId,
                    materialList = materialList
                )
            )
        }
    }
}

