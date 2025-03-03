package view.event.participants

import model.Participant
import model.ParticipantTime

sealed interface ActionsParticipantsPage {
    data class SelectDateOfParticipant(val startMillis: Long, val endMillis: Long) :
        ActionsParticipantsPage

    data class DeleteParticipant(val participantTime: ParticipantTime) : ActionsParticipantsPage
    data class NavigateToEditParticipant(val participantTime: ParticipantTime) :
        ActionsParticipantsPage

    data class AddNewParticipant(val participant: Participant) : ActionsParticipantsPage
    data object Save : ActionsParticipantsPage

    data object GoBack : ActionsParticipantsPage

    data object DismissDatePicker : ActionsParticipantsPage
    data object CloseSearchBar : ActionsParticipantsPage
    data object NavigateToCreateAddOrRemoveParticipantOfEvent : ActionsParticipantsPage
    data object CreateNewParticipant : ActionsParticipantsPage

}