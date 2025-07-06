package view.event.new_meal_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Participant
import view.admin.new_participant.ActionsNewParticipant
import view.shared.ResultState

data class AllParticipantsState(
    val allParticipants: List<Participant> = listOf(),
    val availableGroups: Set<String> = setOf()
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
                    val sortedParticipants = participants.sortedBy { it.firstName.lowercase() }
                    val availableGroups = participants
                        .mapNotNull { it.selectedGroup.takeIf { group -> group.isNotBlank() } }
                        .toSet()
                    _state.value = ResultState.Success(
                        AllParticipantsState(
                            allParticipants = sortedParticipants,
                            availableGroups = availableGroups
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.e("Error loading participants", e)
            }
        }
    }

    fun addParticipant(participant: Participant) {
        val currentState = state.value.getSuccessData() ?: AllParticipantsState()
        val newParticipants = currentState.allParticipants.toMutableList()
        newParticipants.add(participant)

        // Sort participants by firstName
        val sortedParticipants = newParticipants.sortedBy { it.firstName.lowercase() }

        val availableGroups = currentState.availableGroups.toMutableSet()
        if (participant.selectedGroup.isNotBlank()) {
            availableGroups.add(participant.selectedGroup)
        }

        _state.value = ResultState.Success(
            AllParticipantsState(
                allParticipants = sortedParticipants,
                availableGroups = availableGroups
            )
        )
    }

    fun onAction(action: ActionsNewParticipant) {
        when (action) {
            is ActionsNewParticipant.DeleteParticipant -> deleteParticipantFromViewModelToReflectUiChange(
                action.participantId
            )
        }
    }

    private fun deleteParticipantFromViewModelToReflectUiChange(participantId: String) {
        Logger.i("Deleted participant with ID: $participantId")
        val state = state.value.getSuccessData() ?: return;
        val updatedParticipants =
            state.allParticipants.filter { participant -> participant.uid != participantId }

        _state.value = ResultState.Success(
            AllParticipantsState(
                allParticipants = updatedParticipants,
                availableGroups = state.availableGroups
            )
        )
    }
}
