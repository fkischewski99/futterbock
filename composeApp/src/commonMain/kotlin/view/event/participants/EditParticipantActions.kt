package view.event.participants

import model.Participant
import model.ParticipantTime
import view.event.actions.BaseAction
import view.event.actions.EditEventActions

interface EditParticipantActions : BaseAction {
    data object UpdateAllMeals : EditParticipantActions

    data class DeleteParticipant(val participant: ParticipantTime) : EditParticipantActions
    data class AddParticipant(val participant: Participant) : EditParticipantActions
    data class SelectDateOfParticipant(
        val selectedParticipant: ParticipantTime?,
        val startMillis: Long,
        val endMillis: Long
    ) : EditParticipantActions

}