package data

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import model.Event
import model.Ingredient
import model.Meal
import model.Participant
import model.ParticipantTime
import model.Recipe
import model.ShoppingIngredient

interface EventRepository {

    // New Not Realm Functions
    //Event
    suspend fun deleteEvent(eventId: String)
    suspend fun getEventById(eventId: String): Event?
    suspend fun createNewEvent(): Event
    suspend fun saveExistingEvent(futureEvent: Event)
    suspend fun getEventList(group: String): Flow<List<Event>>


    suspend fun getNumberOfParticipants(eventId: String): Int
    suspend fun getParticipantsOfEvent(
        eventId: String,
        withParticipant: Boolean
    ): List<ParticipantTime>

    suspend fun getAllParticipantsOfStamm(): Flow<List<Participant>>
    suspend fun deleteParticipantOfEvent(eventId: String, participantId: String)
    suspend fun addParticipantToEvent(newParticipant: Participant, event: Event): ParticipantTime
    suspend fun createNewParticipant(participant: Participant)

    suspend fun getAllRecipes(): List<Recipe>
    suspend fun getMealById(eventId: String, mealId: String): Meal
    suspend fun getRecipeById(recipeId: String): Recipe

    suspend fun getAllMealsOfEvent(eventId: String): List<Meal>
    suspend fun getShoppingIngredients(eventId: String): List<ShoppingIngredient>
    suspend fun createNewMeal(eventId: String, day: Instant): Meal
    suspend fun deleteMeal(eventId: String, mealId: String)
    suspend fun updateMeal(eventId: String, meal: Meal)

    suspend fun updateParticipantTime(
        eventId: String,
        participant: ParticipantTime,
    )

    suspend fun getIngredientById(ingredientId: String): Ingredient
    suspend fun saveShoppingList(eventId: String, shoppingList: List<ShoppingIngredient>)
    suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal>

    suspend fun getAllIngredients(): List<Ingredient>
}