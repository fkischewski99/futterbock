package view.event.cooking_groups

import model.ParticipantTime
import view.event.actions.BaseAction

sealed interface CookingGroupActions : BaseAction {
    data class RenameGroup(val oldName: String, val newName: String) : CookingGroupActions
    data class MergeGroups(val sourceGroups: List<String>, val targetGroupName: String) :
        CookingGroupActions

    data class MoveBetweenGroups(val participant: ParticipantTime, val targetGroup: String) :
        CookingGroupActions

    data class UpdateParticipantCookingGroup(
        val participant: ParticipantTime,
        val newGroup: String
    ) : CookingGroupActions
}