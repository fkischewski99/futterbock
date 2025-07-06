package view.event.cooking_groups

import co.touchlab.kermit.Logger
import data.EventRepository
import model.ParticipantTime
import view.event.EventState
import view.shared.ResultState

class HandleCookingGroupActions(
    private val eventRepository: EventRepository
) {
    suspend fun handleAction(
        currentState: EventState,
        action: CookingGroupActions
    ): ResultState<EventState> {
        try {
            return when (action) {
                is CookingGroupActions.RenameGroup -> renameGroup(
                    currentState = currentState,
                    oldName = action.oldName,
                    newName = action.newName
                )

                is CookingGroupActions.MergeGroups -> mergeGroups(
                    currentState = currentState,
                    sourceGroups = action.sourceGroups,
                    targetGroupName = action.targetGroupName
                )

                is CookingGroupActions.MoveBetweenGroups -> moveBetweenGroups(
                    currentState = currentState,
                    participant = action.participant,
                    targetGroup = action.targetGroup
                )

                is CookingGroupActions.UpdateParticipantCookingGroup -> updateParticipantCookingGroup(
                    currentState = currentState,
                    participant = action.participant,
                    newGroup = action.newGroup
                )
            }
        } catch (e: Exception) {
            Logger.e("Error handling cooking group action", e)
            return ResultState.Error("Fehler beim Bearbeiten der Kochgruppen")
        }
    }

    private suspend fun renameGroup(
        currentState: EventState,
        oldName: String,
        newName: String
    ): ResultState<EventState> {
        if (newName.isBlank() || oldName == newName) {
            return ResultState.Success(currentState)
        }

        // Only take participants who are actually in the specified group
        // Don't include participants without a group (empty cookingGroup)
        val participantsToUpdate = currentState.participantList.filter { participant ->
            participant.cookingGroup == oldName && participant.cookingGroup.isNotEmpty()
        }

        val updatedParticipants = currentState.participantList.map { participant ->
            if (participantsToUpdate.contains(participant)) {
                participant.apply { cookingGroup = newName }
            } else {
                participant
            }
        }

        // Update participants in database
        participantsToUpdate.forEach { participant ->
            participant.cookingGroup = newName
            eventRepository.updateParticipantTime(currentState.event.uid, participant)
        }

        return ResultState.Success(
            currentState.copy(participantList = updatedParticipants)
        )
    }

    private suspend fun mergeGroups(
        currentState: EventState,
        sourceGroups: List<String>,
        targetGroupName: String
    ): ResultState<EventState> {
        if (targetGroupName.isBlank()) {
            return ResultState.Success(currentState)
        }

        val participantsToUpdate = currentState.participantList.filter { participant ->
            sourceGroups.contains(participant.cookingGroup) ||
                    (participant.cookingGroup.isEmpty() && sourceGroups.contains("Keine Gruppe"))
        }

        val updatedParticipants = currentState.participantList.map { participant ->
            if (participantsToUpdate.contains(participant)) {
                participant.apply { cookingGroup = targetGroupName }
            } else {
                participant
            }
        }

        // Update participants in database
        participantsToUpdate.forEach { participant ->
            participant.cookingGroup = targetGroupName
            eventRepository.updateParticipantTime(currentState.event.uid, participant)
        }

        return ResultState.Success(
            currentState.copy(participantList = updatedParticipants)
        )
    }

    private suspend fun moveBetweenGroups(
        currentState: EventState,
        participant: ParticipantTime,
        targetGroup: String
    ): ResultState<EventState> {
        val updatedParticipants = currentState.participantList.map { p ->
            if (p.uid == participant.uid) {
                p.apply { cookingGroup = targetGroup }
            } else {
                p
            }
        }

        // Update participant in database
        participant.cookingGroup = targetGroup
        eventRepository.updateParticipantTime(currentState.event.uid, participant)

        return ResultState.Success(
            currentState.copy(participantList = updatedParticipants)
        )
    }

    private suspend fun updateParticipantCookingGroup(
        currentState: EventState,
        participant: ParticipantTime,
        newGroup: String
    ): ResultState<EventState> {
        val updatedParticipants = currentState.participantList.map { p ->
            if (p.uid == participant.uid) {
                p.apply { cookingGroup = newGroup }
            } else {
                p
            }
        }

        // Update participant in database
        participant.cookingGroup = newGroup
        eventRepository.updateParticipantTime(currentState.event.uid, participant)

        return ResultState.Success(
            currentState.copy(participantList = updatedParticipants)
        )
    }
}