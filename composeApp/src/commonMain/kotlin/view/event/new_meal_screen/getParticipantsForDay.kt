package view.event.new_meal_screen

import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import model.ParticipantTime

fun getParticipantsForDay(
    allParticipantsOfEvent: List<ParticipantTime>,
    selectedMealDay: Instant
): List<ParticipantTime> {
    val participantsForDay = mutableListOf<ParticipantTime>()
    for (participant in allParticipantsOfEvent) {
        val selectedMealDayLocalDate =
            selectedMealDay.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val participantFromLocalDate =
            participant.from.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val participantToLocalDate =
            participant.to.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        if (selectedMealDayLocalDate in participantFromLocalDate..participantToLocalDate) {
            participantsForDay.add(participant)
        }
    }
    return participantsForDay
}