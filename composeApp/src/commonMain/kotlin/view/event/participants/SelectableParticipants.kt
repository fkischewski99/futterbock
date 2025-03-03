package view.event.participants

import co.touchlab.kermit.Logger
import model.Participant
import model.ParticipantTime

fun getSelectableParticipants(
    allParticipants: List<Participant>,
    participantsOfEvent: List<ParticipantTime>
): List<Participant> {
    if (participantsOfEvent.isEmpty())
        return allParticipants
    Logger.i("Number of all participants: " + allParticipants.size)
    val list1Filtered = allParticipants.filter { participant ->
        participantsOfEvent.none { eventParticipant ->
            participant.uid == eventParticipant.participantRef
        }
    }
    Logger.i("Number of Selectable participants: " + list1Filtered.size)
    return list1Filtered
}