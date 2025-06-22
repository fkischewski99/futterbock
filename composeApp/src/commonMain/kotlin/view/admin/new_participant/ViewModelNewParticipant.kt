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
import model.FoodIntolerance
import model.Participant
import view.shared.ResultState


data class NewParticipantState(
    val participantId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val showDatePicker: Boolean = false,
    val selectedHabit: EatingHabit,
    val birthDate: Instant? = null,
    val foodIntolerance: List<FoodIntolerance> = emptyList(),
    val isNewParticipant: Boolean = true,
    val allergies: List<String> = emptyList(),
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

            is ActionsNewParticipant.AddOrRemoveIntolerance -> addOrRemoveIntolerance(
                actionsNewParticipant.foodIntolerance
            )

            is ActionsNewParticipant.AddOrRemoveAllergy -> addOrRemoveAllergy(
                actionsNewParticipant.allergy
            )

            is ActionsNewParticipant.DeleteParticipant -> deleteParticipant(actionsNewParticipant.participantId)
        }
    }

    private fun addOrRemoveAllergy(allergy: String) {
        val data = state.value.getSuccessData() ?: return
        val updatedAllergies: MutableList<String> = data.allergies.toMutableList();
        if (data.allergies.contains(allergy)) {
            updatedAllergies.remove(allergy)
        } else {
            updatedAllergies.add(allergy)
        }
        _state.value = ResultState.Success(
            data.copy(
                allergies = updatedAllergies
            )
        )
    }

    private fun addOrRemoveIntolerance(foodIntolerance: FoodIntolerance) {
        val data = state.value.getSuccessData() ?: return
        val foodIntolerances: MutableList<FoodIntolerance> = data.foodIntolerance.toMutableList()
        if (data.foodIntolerance.contains(foodIntolerance)) {
            foodIntolerances.remove(foodIntolerance)
        } else {
            foodIntolerances.add(foodIntolerance)
        }
        _state.value = ResultState.Success(
            data.copy(
                foodIntolerance = foodIntolerances
            )
        )
    }

    private fun deleteParticipant(participantId: String) {
        viewModelScope.launch {
            eventRepository.deleteParticipant(participantId)
        }
    }

    private fun saveParticipant() {
        val data = state.value.getSuccessData() ?: return
        if (data.firstName.isEmpty() || data.lastName.isEmpty()) {
            return
        }
        _state.value = ResultState.Loading
        val participant = Participant().apply {
            uid = data.participantId
            firstName = data.firstName
            lastName = data.lastName
            eatingHabit = data.selectedHabit
            birthdate = data.birthDate
            intolerances = data.foodIntolerance.toMutableList()
            allergies = data.allergies
        }
        viewModelScope.launch {
            try {
                if (data.isNewParticipant) {
                    Logger.i("Create new Participant with name ${participant.firstName} ${participant.lastName}")
                    val success = eventRepository.createNewParticipant(participant)
                    if (success == null) {
                        _state.value = ResultState.Error("Teilnehmer konnte nicht erstellt werden")
                        return@launch
                    }
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
                participantId = participant.uid,
                firstName = participant.firstName,
                lastName = participant.lastName,
                selectedHabit = participant.eatingHabit,
                birthDate = participant.birthdate,
                foodIntolerance = participant.intolerances,
                allergies = participant.allergies,
                isNewParticipant = false
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
                isNewParticipant = true
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