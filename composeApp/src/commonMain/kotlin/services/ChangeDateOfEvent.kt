package services

import data.EventRepository
import kotlinx.datetime.Instant
import model.Meal
import model.ParticipantTime

class ChangeDateOfEvent(private val eventRepository: EventRepository) {

    /**
     * Iterates over all Participants and checks if their duration of the stay is in beetween the end and the start date, if not it is adjusted
     */
    suspend fun adjustParticipantDates(
        eventId: String,
        participantList: List<ParticipantTime>,
        newStartDate: Instant,
        newEndDate: Instant
    ) {
        participantList.forEach { participantTime ->
            participantTime.from = newStartDate
            participantTime.to = newEndDate
            eventRepository.updateParticipantTime(eventId, participantTime)
        }

    }

    /**
     * Iterates over all Meals and checks if the date is in beetween the end and the start date, if not it is adjusted
     * Event needs to have the old start end End Date so the new Meal Date can be calculated relative to the old Date
     *
     * The calculation starts with the first Date, so if the duration of the new Dates is less than the original the Meals that ar after the End date will be deleted
     */
    suspend fun adjustMealsDates(
        eventId: String,
        listOfMeals: List<Meal>,
        oldStartDate: Instant,
        newStartDate: Instant,
        newEndDate: Instant
    ): List<Meal> {
        val listOfNewMeals: MutableList<Meal> = mutableListOf()
        val offSet =
            newStartDate.epochSeconds - oldStartDate.epochSeconds;
        listOfMeals.forEach { meal ->
            val newMealEpochSeconds = meal.day.epochSeconds + offSet;
            if (newEndDate.epochSeconds > newMealEpochSeconds) {
                val newMeal = meal.copy(day = Instant.fromEpochSeconds(newMealEpochSeconds))
                eventRepository.updateMeal(eventId, newMeal)
                listOfNewMeals.add(newMeal)
            } else {
                eventRepository.deleteMeal(eventId, meal.uid)
            }
        }
        return listOfNewMeals;
    }
}