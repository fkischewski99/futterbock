package view.event.actions

import kotlinx.datetime.LocalDate
import model.Meal
import org.koin.core.time.TimeInMillis

interface EditEventActions : BaseAction {
    data object SharePdf : EditEventActions
    data object ShareRecipePlanPdf : EditEventActions
    data object SaveEvent : EditEventActions
    data class ChangeEventName(val newName: String) : EditEventActions
    data class ChangeEventDates(val eventStartMillis: Long, val eventEndMillis: Long) :
        EditEventActions

    data class CopyEventToFuture(val start: Long, val end: Long) : EditEventActions, LoadingAction
    data class DeleteMeal(val item: Meal) : EditEventActions
    data class AddNewMeal(val day: LocalDate) : EditEventActions
    data class EditExistingMeal(val item: Meal) : EditEventActions
}