package data

import data.local.RoomRepository
import data.sync.OfflineFirstRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import model.*

class DelegatingRepository(
    private val appModeHolder: AppModeHolder,
    private val firebaseRepository: FireBaseRepository,
    private val roomRepository: RoomRepository,
    private val offlineFirstRepository: OfflineFirstRepository
) : EventRepository {

    private val active: EventRepository
        get() = when (appModeHolder.mode.value) {
            AppMode.OFFLINE_ONLY -> roomRepository
            AppMode.OFFLINE_FIRST -> offlineFirstRepository
        }

    override suspend fun deleteEvent(eventId: String) = active.deleteEvent(eventId)
    override suspend fun getEventById(eventId: String) = active.getEventById(eventId)
    override suspend fun createNewEvent() = active.createNewEvent()
    override suspend fun saveExistingEvent(event: Event) = active.saveExistingEvent(event)
    override suspend fun getEventList(group: String) = active.getEventList(group)
    override suspend fun getNumberOfParticipants(eventId: String) = active.getNumberOfParticipants(eventId)
    override suspend fun getParticipantsOfEvent(eventId: String, withParticipant: Boolean) = active.getParticipantsOfEvent(eventId, withParticipant)
    override suspend fun getAllParticipantsOfStamm() = active.getAllParticipantsOfStamm()
    override suspend fun deleteParticipantOfEvent(eventId: String, participantId: String) = active.deleteParticipantOfEvent(eventId, participantId)
    override suspend fun addParticipantToEvent(newParticipant: Participant, event: Event) = active.addParticipantToEvent(newParticipant, event)
    override suspend fun createNewParticipant(participant: Participant) = active.createNewParticipant(participant)
    override suspend fun updateParticipant(participant: Participant) = active.updateParticipant(participant)
    override suspend fun deleteParticipant(participantId: String) = active.deleteParticipant(participantId)
    override suspend fun getParticipantById(participantId: String) = active.getParticipantById(participantId)
    override suspend fun findParticipantByName(firstName: String, lastName: String) = active.findParticipantByName(firstName, lastName)
    override suspend fun getAllRecipes() = active.getAllRecipes()
    override suspend fun getUserCreatedRecipes() = active.getUserCreatedRecipes()
    override suspend fun getMealById(eventId: String, mealId: String) = active.getMealById(eventId, mealId)
    override suspend fun getRecipeById(recipeId: String) = active.getRecipeById(recipeId)
    override suspend fun createRecipe(recipe: Recipe) = active.createRecipe(recipe)
    override suspend fun updateRecipe(recipe: Recipe) = active.updateRecipe(recipe)
    override suspend fun deleteRecipe(recipeId: String) = active.deleteRecipe(recipeId)
    override suspend fun getAllMealsOfEvent(eventId: String) = active.getAllMealsOfEvent(eventId)
    override suspend fun createNewMeal(eventId: String, day: Instant) = active.createNewMeal(eventId, day)
    override suspend fun deleteMeal(eventId: String, mealId: String) = active.deleteMeal(eventId, mealId)
    override suspend fun updateMeal(eventId: String, meal: Meal) = active.updateMeal(eventId, meal)
    override suspend fun updateParticipantTime(eventId: String, participant: ParticipantTime) = active.updateParticipantTime(eventId, participant)
    override suspend fun getIngredientById(ingredientId: String) = active.getIngredientById(ingredientId)
    override suspend fun getMealsWithRecipeAndIngredients(eventId: String) = active.getMealsWithRecipeAndIngredients(eventId)
    override suspend fun getMultiDayShoppingList(eventId: String) = active.getMultiDayShoppingList(eventId)
    override suspend fun saveMultiDayShoppingList(eventId: String, multiDayShoppingList: MultiDayShoppingList) = active.saveMultiDayShoppingList(eventId, multiDayShoppingList)
    override suspend fun getDailyShoppingList(eventId: String, date: LocalDate) = active.getDailyShoppingList(eventId, date)
    override suspend fun saveDailyShoppingList(eventId: String, date: LocalDate, dailyShoppingList: DailyShoppingList) = active.saveDailyShoppingList(eventId, date, dailyShoppingList)
    override suspend fun updateShoppingIngredientStatus(eventId: String, date: LocalDate, ingredientId: String, completed: Boolean) = active.updateShoppingIngredientStatus(eventId, date, ingredientId, completed)
    override suspend fun deleteShoppingListForDate(eventId: String, date: LocalDate) = active.deleteShoppingListForDate(eventId, date)
    override suspend fun getAllIngredients() = active.getAllIngredients()
    override suspend fun saveMaterialList(eventId: String, materialList: List<Material>) = active.saveMaterialList(eventId, materialList)
    override suspend fun getMaterialListOfEvent(eventId: String) = active.getMaterialListOfEvent(eventId)
    override suspend fun deleteMaterialById(eventId: String, materialId: String) = active.deleteMaterialById(eventId, materialId)
    override suspend fun getAllMaterials() = active.getAllMaterials()
}
