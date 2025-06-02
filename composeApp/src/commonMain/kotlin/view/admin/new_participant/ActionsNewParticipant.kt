package view.admin.new_participant

import model.EatingHabit
import model.FoodIntolerance
import model.Participant
import view.event.actions.BaseAction

interface ActionsNewParticipant : BaseAction {
    data object ShowDatePicker : ActionsNewParticipant
    data object GoBack : ActionsNewParticipant
    data object InitWithoutParticipant : ActionsNewParticipant
    data class InitWithParticipant(val participant: Participant) : ActionsNewParticipant
    data class DeleteParticipant(val participantId: String) : ActionsNewParticipant

    data class ChangeFirstName(val firstName: String) : ActionsNewParticipant
    data class ChangeLastName(val lastName: String) : ActionsNewParticipant
    data class SelectBirthDate(val millis: Long) : ActionsNewParticipant
    data class SelectEatingHabit(val item: EatingHabit) : ActionsNewParticipant
    data class AddOrRemoveIntolerance(val foodIntolerance: FoodIntolerance) :
        ActionsNewParticipant {
    }

    // allergy is the uid of the ingredient
    data class AddOrRemoveAllergy(val allergy: String) :
        ActionsNewParticipant {
    }

    data object Save : ActionsNewParticipant
}