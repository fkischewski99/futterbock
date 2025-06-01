package view.admin.new_participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import model.EatingHabit
import model.Participant
import modules.viewModelModules
import view.event.participants.ActionsParticipantsPage
import view.shared.ResultState


data class NewParticipantState(
    val firstName: String = "",
    val lastName: String = "",
    val showDatePicker: Boolean = false,
    val selectedHabit: EatingHabit,
    val birthDate: Instant? = null,
    val isNewParticipant: Boolean = true
)

class ViewModelNewParticipant(
    private val eventRepository: EventRepository
) : ViewModel() {
    private val _state: MutableStateFlow<ResultState<NewParticipantState>> =
        MutableStateFlow(ResultState.Loading)
    val state = _state.asStateFlow()

    fun onAction(actionsNewParticipant: ActionsNewParticipant) {
        when (actionsNewParticipant) {
            is ActionsNewParticipant.ShowDatePicker -> showDatePicker()
            is ActionsNewParticipant.ChangeLastName -> changeLastName(actionsNewParticipant.lastName)
            is ActionsNewParticipant.ChangeFirstName -> changeFirstName(actionsNewParticipant.firstName)
            is ActionsNewParticipant.SelectBirthDate -> selectDate(actionsNewParticipant.millis)
            is ActionsNewParticipant.SelectEatingHabit -> selectEatingHabit(actionsNewParticipant.item)
            is ActionsNewParticipant.Save -> saveParticipant()
            is ActionsNewParticipant.InitWithoutParticipant -> initializeScreenWithNewParticipant()
            is ActionsNewParticipant.InitWithParticipant -> initializeWithParticipant(
                actionsNewParticipant.participant
            )

            is ActionsNewParticipant.DeleteParticipant -> deleteParticipant(actionsNewParticipant.participantId)
        }
    }

    private fun deleteParticipant(participantId: String) {
        viewModelScope.launch {
            eventRepository.deleteParticipant(participantId)
        }
    }

    private fun saveParticipant() {
        val data = state.value.getSuccessData() ?: return
        _state.value = ResultState.Loading
        val participant = Participant().apply {
            firstName = data.firstName
            lastName = data.lastName
            eatingHabit = data.selectedHabit
            birthdate = data.birthDate
        }
        viewModelScope.launch {
            try {
                if (data.isNewParticipant) {
                    Logger.i("Create new Participant with name ${participant.firstName} ${participant.lastName}")
                    eventRepository.createNewParticipant(participant)
                } else {
                    Logger.i("Update Participant with name ${participant.firstName} ${participant.lastName}")
                    eventRepository.updateParticipant(participant)
                }
            } catch (e: Exception) {
                Logger.e("" + e.message)
                ResultState.Error("Fehler beim anlegen des Teilnehmenden")
            }
            initializeScreenWithNewParticipant()
        }
    }

    private fun initializeWithParticipant(participant: Participant) {
        _state.value = ResultState.Success(
            NewParticipantState(
                firstName = participant.firstName,
                lastName = participant.lastName,
                selectedHabit = participant.eatingHabit,
                birthDate = participant.birthdate,
                isNewParticipant = true
            )
        )
    }


    private fun initializeScreenWithNewParticipant() {
        _state.value = ResultState.Success(
            NewParticipantState(
                firstName = "",
                lastName = "",
                selectedHabit = EatingHabit.OMNIVORE,
                birthDate = null,
                isNewParticipant = false
            )
        )
    }

    private fun changeFirstName(newFirstName: String) {
        val data = state.value.getSuccessData() ?: return
        _state.value = ResultState.Success(
            data.copy(
                firstName = newFirstName
            )
        )
    }

    private fun changeLastName(newLastName: String) {
        val data = state.value.getSuccessData() ?: return
        _state.value = ResultState.Success(
            data.copy(
                lastName = newLastName
            )
        )
    }

    private fun showDatePicker() {
        val data = state.value.getSuccessData() ?: return
        _state.value = ResultState.Success(
            data.copy(
                showDatePicker = !data.showDatePicker
            )
        )
    }

    private fun selectDate(millis: Long) {
        val dateSelect = Instant.fromEpochMilliseconds(millis)
        val data = state.value.getSuccessData() ?: return
        _state.value = ResultState.Success(
            data.copy(
                showDatePicker = false,
                birthDate = dateSelect
            )
        )
    }

    private fun selectEatingHabit(eatingHabit: EatingHabit) {
        val data = state.value.getSuccessData() ?: return
        _state.value = ResultState.Success(
            data.copy(
                selectedHabit = eatingHabit
            )
        )
    }
}