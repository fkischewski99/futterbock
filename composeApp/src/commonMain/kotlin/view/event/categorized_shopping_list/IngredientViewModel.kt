package view.event.categorized_shopping_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Ingredient

class IngredientViewModel(val eventRepository: EventRepository) : ViewModel() {

    private var _state = MutableStateFlow<List<Ingredient>>(listOf())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Logger.i("Request all ingredients")
            _state.value = eventRepository.getAllIngredients()
        }
    }

}