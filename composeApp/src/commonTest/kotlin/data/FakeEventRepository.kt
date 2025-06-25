package data

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import model.Event
import model.Ingredient
import model.Material
import model.Meal
import model.Participant
import model.ParticipantTime
import model.Recipe
import model.ShoppingIngredient

class FakeEventRepository : EventRepository {

    var participants: Map<String, Participant> = mapOf()
    var mealsForEvent: MutableList<Meal> = mutableListOf()
    var materialsForEvent: MutableList<Material> = mutableListOf()
    var shoppingIngredients: MutableList<ShoppingIngredient> = mutableListOf()

    override suspend fun deleteEvent(eventId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getEventById(eventId: String): Event? {
        TODO("Not yet implemented")
    }

    override suspend fun createNewEvent(): Event {
        TODO("Not yet implemented")
    }

    override suspend fun saveExistingEvent(event: Event) {
        TODO("Not yet implemented")
    }

    override suspend fun getEventList(group: String): Flow<List<Event>> {
        TODO("Not yet implemented")
    }

    override suspend fun getNumberOfParticipants(eventId: String): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getParticipantsOfEvent(
        eventId: String,
        withParticipant: Boolean
    ): List<ParticipantTime> {
        return participants.map { (id, participant) ->
            ParticipantTime(
                uid = id,
                from = Clock.System.now(),
                to = Clock.System.now(),
                participant = if (withParticipant) participant else null,
                participantRef = participant.uid
            )
        }
    }

    override suspend fun getAllParticipantsOfStamm(): Flow<List<Participant>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteParticipantOfEvent(eventId: String, participantId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun addParticipantToEvent(
        newParticipant: Participant,
        event: Event
    ): ParticipantTime {
        TODO("Not yet implemented")
    }

    override suspend fun createNewParticipant(participant: Participant): Participant? {
        TODO("Not yet implemented")
    }

    override suspend fun updateParticipant(participant: Participant) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteParticipant(participantId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getParticipantById(participantId: String): Participant? {
        return participants[participantId] ?: Participant().apply {
            uid = participantId
        }
    }

    override suspend fun findParticipantByName(firstName: String, lastName: String): Participant? {
        TODO("Not yet implemented")
    }

    override suspend fun getAllRecipes(): List<Recipe> {
        TODO("Not yet implemented")
    }

    override suspend fun getMealById(eventId: String, mealId: String): Meal {
        TODO("Not yet implemented")
    }

    override suspend fun getRecipeById(recipeId: String): Recipe {
        TODO("Not yet implemented")
    }

    override suspend fun getAllMealsOfEvent(eventId: String): List<Meal> {
        return mealsForEvent
    }

    override suspend fun getShoppingIngredients(eventId: String): List<ShoppingIngredient> {
        return shoppingIngredients
    }

    override suspend fun createNewMeal(eventId: String, day: Instant): Meal {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMeal(eventId: String, mealId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMeal(eventId: String, meal: Meal) {
        TODO("Not yet implemented")
    }

    override suspend fun updateParticipantTime(eventId: String, participant: ParticipantTime) {
        TODO("Not yet implemented")
    }

    override suspend fun getIngredientById(ingredientId: String): Ingredient {
        return Ingredient().apply {
            name = "TestIngredient"
        }
    }

    override suspend fun saveShoppingList(eventId: String, shoppingList: List<ShoppingIngredient>) {
        TODO("Not yet implemented")
    }

    override suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal> {
        return mealsForEvent
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        TODO("Not yet implemented")
    }

    override suspend fun saveMaterialList(eventId: String, materialList: List<Material>) {
        TODO("Not yet implemented")
    }

    override suspend fun getMaterialListOfEvent(eventId: String): List<Material> {
        return materialsForEvent
    }

    override suspend fun deleteMaterialById(eventId: String, materialId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteShoppingListItemById(eventId: String, listItemId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllMaterials(): List<Material> {
        TODO("Not yet implemented")
    }
}