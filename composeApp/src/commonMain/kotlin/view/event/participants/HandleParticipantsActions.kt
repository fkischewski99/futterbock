package view.event.participants

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import model.Meal
import model.MealType
import model.Participant
import model.ParticipantTime
import model.Recipe
import model.RecipeSelection
import view.event.EventState
import view.event.new_event.groupMealsByDate
import view.event.new_meal_screen.getParticipantsForDay
import view.shared.HelperFunctions
import view.shared.ResultState

class HandleParticipantsActions(
    private val eventRepository: EventRepository,
) {
    suspend fun handleAction(
        currentState: EventState,
        editEventActions: EditParticipantActions
    ): ResultState<EventState> {
        try {

            return when (editEventActions) {
                is EditParticipantActions.AddParticipant -> addParticipantToEvent(
                    currentState = currentState,
                    participantToAdd = editEventActions.participant
                )

                is EditParticipantActions.DeleteParticipant -> removeParticipantOfEvent(
                    currentState = currentState,
                    participantToDelete = editEventActions.participant
                )

                is EditParticipantActions.SelectDateOfParticipant -> changeDateOfParticipant(
                    currentState = currentState,
                    participantToChangeDates = editEventActions.selectedParticipant,
                    startDate = editEventActions.startMillis,
                    endDate = editEventActions.endMillis
                )

                is EditParticipantActions.UpdateAllMeals -> updateAllMeals(currentState = currentState)
                else -> {
                    Logger.i(editEventActions.toString())
                    return ResultState.Error("wrong action")
                }
            }
        } catch (e: Exception) {
            return ResultState.Error("Fehler beim bearbeiten der Teilnehmerdaten")
        }
    }

    private suspend fun updateAllMeals(currentState: EventState): ResultState<EventState> {
        currentState.mealList.forEach {
            eventRepository.updateMeal(eventId = currentState.event.uid, it)
        }
        return ResultState.Success(currentState)
    }

    private fun changeDateOfParticipant(
        currentState: EventState,
        participantToChangeDates: ParticipantTime?,
        startDate: Long,
        endDate: Long
    ): ResultState<EventState> {
        val foundParticipant =
            currentState.participantList.find { participantTime -> participantTime.participantRef == participantToChangeDates!!.participantRef }
        foundParticipant!!.from = HelperFunctions.getInstant(startDate)
        foundParticipant.to = HelperFunctions.getInstant(endDate)
        removeParticipantFromMeal(currentState.mealList, foundParticipant)
        return ResultState.Success(currentState)
    }

    private fun removeParticipantFromMeal(mealList: List<Meal>, foundParticipant: ParticipantTime) {
        mealList.forEach { meal: Meal ->
            if (meal.day < foundParticipant.from || meal.day > foundParticipant.to) {
                meal.recipeSelections.forEach { recipeSelection ->
                    recipeSelection.eaterIds.remove(foundParticipant.participantRef)
                }
            }
        }
    }


    private suspend fun removeParticipantOfEvent(
        currentState: EventState,
        participantToDelete: ParticipantTime
    ): ResultState<EventState> {
        val newParticipantList =
            currentState.participantList.filter { participantTime -> participantTime.participantRef != participantToDelete.participantRef }
        for (meal in currentState.mealList) {
            meal.recipeSelections.forEach { recipeSelection ->
                recipeSelection.eaterIds.remove(participantToDelete.participantRef)
            }
        }
        eventRepository.deleteParticipantOfEvent(
            currentState.event.uid,
            participantId = participantToDelete.uid
        )
        return ResultState.Success(
            currentState.copy(
                participantList = newParticipantList,
                currentParticipantsOfMeal = getParticipantsForDay(
                    newParticipantList,
                    currentState.selectedMeal.day
                )
            )
        )
    }


    private suspend fun addParticipantToEvent(
        currentState: EventState,
        participantToAdd: Participant
    ): ResultState<EventState> {
        val newParticipantList = currentState.participantList.toMutableList()
        val participantTime =
            eventRepository.addParticipantToEvent(participantToAdd, currentState.event)
        newParticipantList.add(participantTime)
        for (meal in currentState.mealList) {
            meal.recipeSelections.forEach { recipeSelection ->
                recipeSelection.eaterIds.add(participantToAdd.uid)
            }
        }
        return ResultState.Success(
            currentState.copy(
                participantList = newParticipantList,
                currentParticipantsOfMeal = getParticipantsForDay(
                    newParticipantList,
                    currentState.selectedMeal.day
                )
            )
        )
    }

}