package view.event.new_meal_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Recipe

class RecipeViewModel(val eventRepository: EventRepository) : ViewModel() {

    private var _state = MutableStateFlow<List<Recipe>>(listOf())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Logger.i("Request all recipes")
            _state.value = eventRepository.getAllRecipes()
        }
    }

}