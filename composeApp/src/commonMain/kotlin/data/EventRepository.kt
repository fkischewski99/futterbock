package data

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import model.DailyShoppingList
import model.Event
import model.Ingredient
import model.Material
import model.Meal
import model.MultiDayShoppingList
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
    suspend fun saveExistingEvent(event: Event)
    suspend fun getEventList(group: String): Flow<List<Event>>


    suspend fun getNumberOfParticipants(eventId: String): Int
    suspend fun getParticipantsOfEvent(
        eventId: String,
        withParticipant: Boolean
    ): List<ParticipantTime>

    suspend fun getAllParticipantsOfStamm(): Flow<List<Participant>>
    suspend fun deleteParticipantOfEvent(eventId: String, participantId: String)
    suspend fun addParticipantToEvent(newParticipant: Participant, event: Event): ParticipantTime
    suspend fun createNewParticipant(participant: Participant): Participant?
    suspend fun updateParticipant(participant: Participant)
    suspend fun deleteParticipant(participantId: String)
    suspend fun getParticipantById(participantId: String): Participant?
    suspend fun findParticipantByName(firstName: String, lastName: String): Participant?

    suspend fun getAllRecipes(): List<Recipe>
    suspend fun getMealById(eventId: String, mealId: String): Meal
    suspend fun getRecipeById(recipeId: String): Recipe

    suspend fun getAllMealsOfEvent(eventId: String): List<Meal>
    suspend fun createNewMeal(eventId: String, day: Instant): Meal
    suspend fun deleteMeal(eventId: String, mealId: String)
    suspend fun updateMeal(eventId: String, meal: Meal)

    suspend fun updateParticipantTime(
        eventId: String,
        participant: ParticipantTime,
    )

    suspend fun getIngredientById(ingredientId: String): Ingredient
    suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal>

    // Multi-day shopping list methods
    suspend fun getMultiDayShoppingList(eventId: String): MultiDayShoppingList?
    suspend fun saveMultiDayShoppingList(
        eventId: String,
        multiDayShoppingList: MultiDayShoppingList
    )

    suspend fun getDailyShoppingList(eventId: String, date: LocalDate): DailyShoppingList?
    suspend fun saveDailyShoppingList(
        eventId: String,
        date: LocalDate,
        dailyShoppingList: DailyShoppingList
    )

    suspend fun updateShoppingIngredientStatus(
        eventId: String,
        date: LocalDate,
        ingredientId: String,
        completed: Boolean
    )

    suspend fun deleteShoppingListForDate(eventId: String, date: LocalDate)

    suspend fun getAllIngredients(): List<Ingredient>
    suspend fun saveMaterialList(eventId: String, materialList: List<Material>)
    suspend fun getMaterialListOfEvent(eventId: String): List<Material>
    suspend fun deleteMaterialById(eventId: String, materialId: String)
    suspend fun getAllMaterials(): List<Material>
}