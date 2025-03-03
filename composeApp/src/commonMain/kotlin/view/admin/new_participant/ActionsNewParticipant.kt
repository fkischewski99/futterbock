package view.admin.new_participant

import model.EatingHabit
import view.event.actions.BaseAction

interface ActionsNewParticipant : BaseAction {
    data object ShowDatePicker : ActionsNewParticipant
    data object GoBack : ActionsNewParticipant
    data object InitWithoutParticipant : ActionsNewParticipant

    data class ChangeFirstName(val firstName: String) : ActionsNewParticipant
    data class ChangeLastName(val lastName: String) : ActionsNewParticipant
    data class SelectBirthDate(val millis: Long) : ActionsNewParticipant
    data class SelectEatingHabit(val item: EatingHabit) : ActionsNewParticipant
    data object Save : ActionsNewParticipant
}