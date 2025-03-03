package view.event.new_meal_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Participant

class AllParticipantsViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow<List<Participant>>(emptyList())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Logger.i("Request all participants for event")
            try {
                eventRepository.getAllParticipantsOfStamm().collect { participants ->
                    _state.value = participants
                }
            } catch (e: Exception) {
                Logger.e("Error loading participants", e)
            }
        }
    }
}
