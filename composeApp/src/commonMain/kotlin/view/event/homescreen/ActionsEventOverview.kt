package view.event.homescreen

sealed interface ActionsEventOverview {
    data object Logout : ActionsEventOverview
    data object NewEvent : ActionsEventOverview
    data object Init : ActionsEventOverview
    data class DeleteEvent(val eventId: String) : ActionsEventOverview

    // Navigation
    data class EditEvent(val eventId: String) : ActionsEventOverview
    data object ShowUserScreen : ActionsEventOverview
}