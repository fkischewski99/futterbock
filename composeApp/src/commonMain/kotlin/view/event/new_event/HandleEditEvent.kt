package view.event.new_event

import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.datetime.LocalDate
import model.Event
import model.Meal
import services.ChangeDateOfEvent
import services.event.eventIsEditable
import services.pdfService.PdfServiceModule
import view.event.EventState
import view.event.actions.EditEventActions
import view.event.new_meal_screen.generateDateRange
import view.event.new_meal_screen.getParticipantsForDay
import view.shared.HelperFunctions
import view.shared.ResultState

class HandleEditEvent(
    private val eventRepository: EventRepository,
    private val changeDateOfEvent: ChangeDateOfEvent,
    private val pdfServiceModule: PdfServiceModule
) {
    suspend fun handleAction(
        currentState: EventState,
        editEventActions: EditEventActions
    ): ResultState<EventState> {
        return when (editEventActions) {
            is EditEventActions.ChangeEventName -> changeEventName(
                currentState,
                editEventActions.newName
            )

            is EditEventActions.ChangeEventDates -> editEventDates(
                oldState = currentState,
                startMillis = editEventActions.eventStartMillis,
                endMillis = editEventActions.eventEndMillis
            )

            is EditEventActions.SaveEvent -> saveEvent(currentState = currentState)
            is EditEventActions.DeleteMeal -> deleteMeal(
                currentState = currentState,
                mealToDelete = editEventActions.item
            )

            is EditEventActions.AddNewMeal -> createNewMeal(
                day = editEventActions.day,
                oldState = currentState
            )

            is EditEventActions.EditExistingMeal -> editMeal(
                meal = editEventActions.item,
                oldState = currentState
            )

            is EditEventActions.SharePdf -> onPdfShare(currentState = currentState)
            is EditEventActions.CopyEventToFuture -> copyEventToFuture(
                eventRepository = eventRepository,
                oldState = currentState,
                startMillis = editEventActions.start,
                endMillis = editEventActions.end
            )

            else -> {
                Logger.i(editEventActions.toString())
                return ResultState.Error("wrong action")
            }
        }
    }

    private suspend fun onPdfShare(currentState: EventState): ResultState.Success<EventState> {
        pdfServiceModule.createPdf(eventId = currentState.event.uid)
        return ResultState.Success(currentState)
    }


    private suspend fun deleteMeal(
        currentState: EventState,
        mealToDelete: Meal
    ): ResultState<EventState> {
        val newMealList =
            currentState.mealList.filter { meal: Meal -> meal.uid != mealToDelete.uid }
        eventRepository.deleteMeal(currentState.event.uid, mealToDelete.uid)

        return ResultState.Success(
            currentState.copy(
                mealList = newMealList,
                mealsGroupedByDate = groupMealsByDate(
                    currentState.event.from,
                    currentState.event.to,
                    newMealList
                )
            )
        )
    }

    private fun editMeal(meal: Meal, oldState: EventState): ResultState.Success<EventState> {
        return updateMealState(meal, oldState)
    }

    private fun updateMealState(meal: Meal, oldState: EventState): ResultState.Success<EventState> {
        return ResultState.Success(
            oldState.copy(
                selectedMeal = meal,
                currentParticipantsOfMeal = getParticipantsForDay(
                    oldState.participantList,
                    meal.day
                ),
                dateRange = generateDateRange(oldState.event.from, oldState.event.to)
            )
        )
    }

    private suspend fun createNewMeal(
        day: LocalDate,
        oldState: EventState
    ): ResultState.Success<EventState> {
        val newMealList = oldState.mealList.toMutableList()
        val meal =
            eventRepository.createNewMeal(
                oldState.event.uid, HelperFunctions.getInstant(day)
            )
        newMealList.add(meal)
        return ResultState.Success(
            oldState.copy(
                mealList = newMealList,
                mealsGroupedByDate = groupMealsByDate(
                    oldState.event.from,
                    oldState.event.to,
                    newMealList
                ),
                selectedMeal = meal,
                currentParticipantsOfMeal = getParticipantsForDay(
                    oldState.participantList,
                    meal.day
                ),
                dateRange = generateDateRange(oldState.event.from, oldState.event.to)
            )
        )
    }


    private fun changeEventName(
        oldState: EventState,
        newName: String
    ): ResultState.Success<EventState> {
        val event = oldState.event
        event.name = newName
        return ResultState.Success(
            oldState.copy(
                event = event
            )
        )
    }

    private suspend fun editEventDates(
        oldState: EventState,
        startMillis: Long,
        endMillis: Long
    ): ResultState<EventState> {
        return if (!eventIsEditable(oldState.event.to)) {
            copyEventToFuture(eventRepository, oldState, startMillis, endMillis)
        } else {
            changeDatesOfEvent(startMillis, endMillis, oldState.event, oldState)
        }
    }

    private suspend fun saveEvent(currentState: EventState): ResultState.Success<EventState> {
        eventRepository.saveExistingEvent(currentState.event)
        return ResultState.Success(currentState)

    }

    private suspend fun copyEventToFuture(
        eventRepository: EventRepository,
        oldState: EventState,
        startMillis: Long,
        endMillis: Long
    ): ResultState<EventState> {
        Logger.i("Copy event to future")
        val futureEvent = eventRepository.createNewEvent()
        futureEvent.name = "Kopie von: " + oldState.event.name
        return changeDatesOfEvent(startMillis, endMillis, futureEvent, oldState)
    }

    private suspend fun changeDatesOfEvent(
        startMillis: Long,
        endMillis: Long,
        event: Event,
        oldState: EventState
    ): ResultState.Success<EventState> {
        val startDateSelect = HelperFunctions.getInstant(startMillis)
        val endDateSelect = HelperFunctions.getInstant(endMillis)
        val oldStartDate = oldState.event.from
        event.from = startDateSelect
        event.to = endDateSelect

        Logger.i("update dates of event")
        changeDateOfEvent.adjustParticipantDates(
            event.uid,
            participantList = oldState.participantList,
            startDateSelect,
            endDateSelect
        )
        val listOfMeals = changeDateOfEvent.adjustMealsDates(
            eventId = event.uid,
            listOfMeals = oldState.mealList,
            oldStartDate = oldStartDate,
            newStartDate = startDateSelect,
            newEndDate = endDateSelect
        )
        saveEvent(oldState)
        val updatedMeals = groupMealsByDate(
            startDateSelect,
            endDateSelect, listOfMeals
        )
        Logger.i("Changed dates sucessfully")
        return ResultState.Success(
            oldState.copy(
                event = event,
                mealList = listOfMeals,
                mealsGroupedByDate = updatedMeals,
                participantList = oldState.participantList
            )
        )
    }
}