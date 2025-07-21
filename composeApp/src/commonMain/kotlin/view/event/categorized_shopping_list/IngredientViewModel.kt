package view.event.categorized_shopping_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Ingredient
import view.shared.ResultState

class IngredientViewModel(val eventRepository: EventRepository) : ViewModel() {

    private var _state = MutableStateFlow<ResultState<List<Ingredient>>>(ResultState.Loading)
    val state = _state.asStateFlow()

    init {
        loadIngredients()
    }

    private fun loadIngredients() {
        viewModelScope.launch {
            try {
                Logger.i("Request all ingredients")
                _state.value = ResultState.Loading
                val ingredients = eventRepository.getAllIngredients()
                _state.value = ResultState.Success(ingredients)
            } catch (e: Exception) {
                Logger.e("Error loading ingredients", e)
                _state.value = ResultState.Error(e.message ?: "Unbekannter Fehler beim Laden der Zutaten")
            }
        }
    }

}