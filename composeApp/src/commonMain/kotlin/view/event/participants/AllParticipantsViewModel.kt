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
import view.shared.ResultState

data class AllParticipantsState(
    val allParticipants: List<Participant> = listOf()
)

class AllParticipantsViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state: MutableStateFlow<ResultState<AllParticipantsState>> =
        MutableStateFlow(ResultState.Loading);
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Logger.i("Request all participants for event")
            try {
                eventRepository.getAllParticipantsOfStamm().collect { participants ->
                    _state.value =
                        ResultState.Success(AllParticipantsState(allParticipants = participants))
                }
            } catch (e: Exception) {
                Logger.e("Error loading participants", e)
            }
        }
    }

    fun addParticipant(participant: Participant) {
        val newParticipants =
            state.value.getSuccessData()?.allParticipants?.toMutableList() ?: mutableListOf()
        newParticipants.add(participant)
        _state.value = ResultState.Success(AllParticipantsState(allParticipants = newParticipants))
    }
}
