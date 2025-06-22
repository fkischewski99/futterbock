package view.navigation

import kotlinx.serialization.Serializable

interface Routes {
    @Serializable
    object Login

    @Serializable
    object Register

    @Serializable
    object Home

    @Serializable
    object EditEventSubGraph

    @Serializable
    object EditEvent

    @Serializable
    data class ShoppingList(val eventId: String) : Routes

    @Serializable
    object MaterialList : Routes

    @Serializable
    class RecipeOverview(val recipeRef: String) : Routes

    @Serializable
    object LoadingScreen

    @Serializable
    object EditMeal : Routes

    @Serializable
    object CreateOrEditParticipant : Routes

    @Serializable
    data object ParticipantsOfEvent : Routes

    @Serializable
    object AddOrRemoveParticipantsOfEvent : Routes

    @Serializable
    object ParticipantAdministration : Routes

    @Serializable
    object CsvImport : Routes
}
