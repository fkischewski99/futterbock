package data.sync

import co.touchlab.kermit.Logger
import data.EventRepository
import data.FireBaseRepository
import data.local.AppDatabase
import data.local.RoomRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*

class OfflineFirstRepository(
    private val db: AppDatabase,
    private val firebaseRepository: FireBaseRepository,
    private val roomRepository: RoomRepository,
    private val networkMonitor: NetworkMonitor
) : EventRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val isOnline: Boolean get() = networkMonitor.isOnline.value

    private suspend fun queueOperation(
        type: String,
        entityId: String,
        parentId: String? = null,
        payload: String = ""
    ) {
        db.pendingOperationDao().insert(
            PendingOperation(
                operationType = type,
                entityId = entityId,
                parentId = parentId,
                payloadJson = payload,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    private suspend fun writeToFirebaseOrQueue(
        operationType: String,
        entityId: String,
        parentId: String? = null,
        payload: String = "",
        firebaseAction: suspend () -> Unit
    ) {
        if (isOnline) {
            try { firebaseAction() }
            catch (e: Exception) { queueOperation(operationType, entityId, parentId, payload) }
        } else {
            queueOperation(operationType, entityId, parentId, payload)
        }
    }

    // --- Events ---

    override suspend fun deleteEvent(eventId: String) {
        roomRepository.deleteEvent(eventId)
        writeToFirebaseOrQueue("DELETE_EVENT", eventId) {
            firebaseRepository.deleteEvent(eventId)
        }
    }

    override suspend fun getEventById(eventId: String): Event? =
        roomRepository.getEventById(eventId)

    override suspend fun createNewEvent(): Event {
        val event = roomRepository.createNewEvent()
        writeToFirebaseOrQueue("CREATE_EVENT", event.uid, payload = json.encodeToString(event)) {
            firebaseRepository.saveExistingEvent(event)
        }
        return event
    }

    override suspend fun saveExistingEvent(event: Event) {
        roomRepository.saveExistingEvent(event)
        writeToFirebaseOrQueue("UPDATE_EVENT", event.uid, payload = json.encodeToString(event)) {
            firebaseRepository.saveExistingEvent(event)
        }
    }

    override suspend fun getEventList(group: String): Flow<List<Event>> =
        roomRepository.getEventList(group)

    // --- Participants ---

    override suspend fun getNumberOfParticipants(eventId: String): Int =
        roomRepository.getNumberOfParticipants(eventId)

    override suspend fun getParticipantsOfEvent(eventId: String, withParticipant: Boolean): List<ParticipantTime> =
        roomRepository.getParticipantsOfEvent(eventId, withParticipant)

    override suspend fun getAllParticipantsOfStamm(): Flow<List<Participant>> =
        roomRepository.getAllParticipantsOfStamm()

    override suspend fun deleteParticipantOfEvent(eventId: String, participantId: String) {
        roomRepository.deleteParticipantOfEvent(eventId, participantId)
        writeToFirebaseOrQueue("DELETE_PARTICIPANT_OF_EVENT", participantId, parentId = eventId) {
            firebaseRepository.deleteParticipantOfEvent(eventId, participantId)
        }
    }

    override suspend fun addParticipantToEvent(newParticipant: Participant, event: Event): ParticipantTime {
        val pt = roomRepository.addParticipantToEvent(newParticipant, event)
        writeToFirebaseOrQueue("ADD_PARTICIPANT_TO_EVENT", pt.uid, parentId = event.uid, payload = json.encodeToString(pt)) {
            firebaseRepository.addParticipantToEvent(newParticipant, event)
        }
        return pt
    }

    override suspend fun createNewParticipant(participant: Participant): Participant? {
        val result = roomRepository.createNewParticipant(participant) ?: return null
        writeToFirebaseOrQueue("CREATE_PARTICIPANT", result.uid, payload = json.encodeToString(result)) {
            firebaseRepository.createNewParticipant(result)
        }
        return result
    }

    override suspend fun updateParticipant(participant: Participant) {
        roomRepository.updateParticipant(participant)
        writeToFirebaseOrQueue("UPDATE_PARTICIPANT", participant.uid, payload = json.encodeToString(participant)) {
            firebaseRepository.updateParticipant(participant)
        }
    }

    override suspend fun deleteParticipant(participantId: String) {
        roomRepository.deleteParticipant(participantId)
        writeToFirebaseOrQueue("DELETE_PARTICIPANT", participantId) {
            firebaseRepository.deleteParticipant(participantId)
        }
    }

    override suspend fun getParticipantById(participantId: String): Participant? =
        roomRepository.getParticipantById(participantId)

    override suspend fun findParticipantByName(firstName: String, lastName: String): Participant? =
        roomRepository.findParticipantByName(firstName, lastName)

    override suspend fun updateParticipantTime(eventId: String, participant: ParticipantTime) {
        roomRepository.updateParticipantTime(eventId, participant)
        writeToFirebaseOrQueue("UPDATE_PARTICIPANT_TIME", participant.uid, parentId = eventId, payload = json.encodeToString(participant)) {
            firebaseRepository.updateParticipantTime(eventId, participant)
        }
    }

    // --- Recipes ---

    override suspend fun getAllRecipes(): List<Recipe> =
        roomRepository.getAllRecipes()

    override suspend fun getUserCreatedRecipes(): List<Recipe> =
        roomRepository.getUserCreatedRecipes()

    override suspend fun getRecipeById(recipeId: String): Recipe? =
        roomRepository.getRecipeById(recipeId)

    override suspend fun createRecipe(recipe: Recipe) {
        roomRepository.createRecipe(recipe)
        writeToFirebaseOrQueue("CREATE_RECIPE", recipe.uid, payload = json.encodeToString(recipe)) {
            firebaseRepository.createRecipe(recipe)
        }
    }

    override suspend fun updateRecipe(recipe: Recipe) {
        roomRepository.updateRecipe(recipe)
        writeToFirebaseOrQueue("UPDATE_RECIPE", recipe.uid, payload = json.encodeToString(recipe)) {
            firebaseRepository.updateRecipe(recipe)
        }
    }

    override suspend fun deleteRecipe(recipeId: String) {
        roomRepository.deleteRecipe(recipeId)
        writeToFirebaseOrQueue("DELETE_RECIPE", recipeId) {
            firebaseRepository.deleteRecipe(recipeId)
        }
    }

    // --- Meals ---

    override suspend fun getAllMealsOfEvent(eventId: String): List<Meal> =
        roomRepository.getAllMealsOfEvent(eventId)

    override suspend fun getMealById(eventId: String, mealId: String): Meal =
        roomRepository.getMealById(eventId, mealId)

    override suspend fun createNewMeal(eventId: String, day: Instant): Meal {
        val meal = roomRepository.createNewMeal(eventId, day)
        writeToFirebaseOrQueue("CREATE_MEAL", meal.uid, parentId = eventId, payload = json.encodeToString(meal)) {
            firebaseRepository.updateMeal(eventId, meal)
        }
        return meal
    }

    override suspend fun deleteMeal(eventId: String, mealId: String) {
        roomRepository.deleteMeal(eventId, mealId)
        writeToFirebaseOrQueue("DELETE_MEAL", mealId, parentId = eventId) {
            firebaseRepository.deleteMeal(eventId, mealId)
        }
    }

    override suspend fun updateMeal(eventId: String, meal: Meal) {
        roomRepository.updateMeal(eventId, meal)
        writeToFirebaseOrQueue("UPDATE_MEAL", meal.uid, parentId = eventId, payload = json.encodeToString(meal)) {
            firebaseRepository.updateMeal(eventId, meal)
        }
    }

    // --- Ingredients ---

    override suspend fun getIngredientById(ingredientId: String): Ingredient =
        roomRepository.getIngredientById(ingredientId)

    override suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal> =
        roomRepository.getMealsWithRecipeAndIngredients(eventId)

    override suspend fun getAllIngredients(): List<Ingredient> =
        roomRepository.getAllIngredients()

    // --- Shopping Lists ---

    override suspend fun getMultiDayShoppingList(eventId: String): MultiDayShoppingList? =
        roomRepository.getMultiDayShoppingList(eventId)

    override suspend fun saveMultiDayShoppingList(eventId: String, multiDayShoppingList: MultiDayShoppingList) {
        roomRepository.saveMultiDayShoppingList(eventId, multiDayShoppingList)
        writeToFirebaseOrQueue("SAVE_MULTI_DAY_SHOPPING_LIST", eventId, parentId = eventId, payload = json.encodeToString(multiDayShoppingList)) {
            firebaseRepository.saveMultiDayShoppingList(eventId, multiDayShoppingList)
        }
    }

    override suspend fun getDailyShoppingList(eventId: String, date: LocalDate): DailyShoppingList? =
        roomRepository.getDailyShoppingList(eventId, date)

    override suspend fun saveDailyShoppingList(eventId: String, date: LocalDate, dailyShoppingList: DailyShoppingList) {
        val multiDayList = getMultiDayShoppingList(eventId)
        if (multiDayList != null) {
            val updated = multiDayList.copy(dailyLists = multiDayList.dailyLists.toMutableMap().apply { put(date, dailyShoppingList) })
            saveMultiDayShoppingList(eventId, updated)
        }
    }

    override suspend fun updateShoppingIngredientStatus(eventId: String, date: LocalDate, ingredientId: String, completed: Boolean) {
        val dailyList = getDailyShoppingList(eventId, date) ?: return
        val updatedIngredients = dailyList.ingredients.map { ingredient ->
            if (ingredient.uid == ingredientId || ingredient.ingredientRef == ingredientId) {
                ingredient.apply { shoppingDone = completed }
            } else ingredient
        }
        saveDailyShoppingList(eventId, date, dailyList.copy(ingredients = updatedIngredients))
    }

    override suspend fun deleteShoppingListForDate(eventId: String, date: LocalDate) {
        val multiDayList = getMultiDayShoppingList(eventId) ?: return
        val updated = multiDayList.copy(dailyLists = multiDayList.dailyLists.toMutableMap().apply { remove(date) })
        saveMultiDayShoppingList(eventId, updated)
    }

    // --- Materials ---

    override suspend fun saveMaterialList(eventId: String, materialList: List<Material>) {
        roomRepository.saveMaterialList(eventId, materialList)
        writeToFirebaseOrQueue("SAVE_MATERIAL_LIST", eventId, parentId = eventId, payload = json.encodeToString(materialList)) {
            firebaseRepository.saveMaterialList(eventId, materialList)
        }
    }

    override suspend fun getMaterialListOfEvent(eventId: String): List<Material> =
        roomRepository.getMaterialListOfEvent(eventId)

    override suspend fun deleteMaterialById(eventId: String, materialId: String) {
        roomRepository.deleteMaterialById(eventId, materialId)
        writeToFirebaseOrQueue("DELETE_MATERIAL", materialId, parentId = eventId) {
            firebaseRepository.deleteMaterialById(eventId, materialId)
        }
    }

    override suspend fun getAllMaterials(): List<Material> =
        roomRepository.getAllMaterials()
}
