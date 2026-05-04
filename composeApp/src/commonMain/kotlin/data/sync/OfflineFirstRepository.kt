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

    // --- Events ---

    override suspend fun deleteEvent(eventId: String) {
        roomRepository.deleteEvent(eventId)
        if (isOnline) {
            try { firebaseRepository.deleteEvent(eventId) }
            catch (e: Exception) { queueOperation("DELETE_EVENT", eventId) }
        } else {
            queueOperation("DELETE_EVENT", eventId)
        }
    }

    override suspend fun getEventById(eventId: String): Event? {
        if (isOnline) {
            try {
                val event = firebaseRepository.getEventById(eventId)
                if (event != null) roomRepository.saveExistingEvent(event)
                return event
            } catch (_: Exception) {}
        }
        return roomRepository.getEventById(eventId)
    }

    override suspend fun createNewEvent(): Event {
        val event = roomRepository.createNewEvent()
        if (isOnline) {
            try { firebaseRepository.saveExistingEvent(event) }
            catch (e: Exception) { queueOperation("CREATE_EVENT", event.uid, payload = json.encodeToString(event)) }
        } else {
            queueOperation("CREATE_EVENT", event.uid, payload = json.encodeToString(event))
        }
        return event
    }

    override suspend fun saveExistingEvent(event: Event) {
        roomRepository.saveExistingEvent(event)
        if (isOnline) {
            try { firebaseRepository.saveExistingEvent(event) }
            catch (e: Exception) { queueOperation("UPDATE_EVENT", event.uid, payload = json.encodeToString(event)) }
        } else {
            queueOperation("UPDATE_EVENT", event.uid, payload = json.encodeToString(event))
        }
    }

    override suspend fun getEventList(group: String): Flow<List<Event>> {
        if (isOnline) {
            try {
                return firebaseRepository.getEventList(group)
            } catch (_: Exception) {}
        }
        return roomRepository.getEventList(group)
    }

    // --- Participants ---

    override suspend fun getNumberOfParticipants(eventId: String): Int {
        if (isOnline) {
            try { return firebaseRepository.getNumberOfParticipants(eventId) }
            catch (_: Exception) {}
        }
        return roomRepository.getNumberOfParticipants(eventId)
    }

    override suspend fun getParticipantsOfEvent(eventId: String, withParticipant: Boolean): List<ParticipantTime> {
        if (isOnline) {
            try { return firebaseRepository.getParticipantsOfEvent(eventId, withParticipant) }
            catch (_: Exception) {}
        }
        return roomRepository.getParticipantsOfEvent(eventId, withParticipant)
    }

    override suspend fun getAllParticipantsOfStamm(): Flow<List<Participant>> {
        if (isOnline) {
            try { return firebaseRepository.getAllParticipantsOfStamm() }
            catch (_: Exception) {}
        }
        return roomRepository.getAllParticipantsOfStamm()
    }

    override suspend fun deleteParticipantOfEvent(eventId: String, participantId: String) {
        roomRepository.deleteParticipantOfEvent(eventId, participantId)
        if (isOnline) {
            try { firebaseRepository.deleteParticipantOfEvent(eventId, participantId) }
            catch (e: Exception) { queueOperation("DELETE_PARTICIPANT_OF_EVENT", participantId, parentId = eventId) }
        } else {
            queueOperation("DELETE_PARTICIPANT_OF_EVENT", participantId, parentId = eventId)
        }
    }

    override suspend fun addParticipantToEvent(newParticipant: Participant, event: Event): ParticipantTime {
        val pt = roomRepository.addParticipantToEvent(newParticipant, event)
        if (isOnline) {
            try { firebaseRepository.addParticipantToEvent(newParticipant, event) }
            catch (e: Exception) { queueOperation("ADD_PARTICIPANT_TO_EVENT", pt.uid, parentId = event.uid, payload = json.encodeToString(pt)) }
        } else {
            queueOperation("ADD_PARTICIPANT_TO_EVENT", pt.uid, parentId = event.uid, payload = json.encodeToString(pt))
        }
        return pt
    }

    override suspend fun createNewParticipant(participant: Participant): Participant? {
        val result = roomRepository.createNewParticipant(participant) ?: return null
        if (isOnline) {
            try { firebaseRepository.createNewParticipant(result) }
            catch (e: Exception) { queueOperation("CREATE_PARTICIPANT", result.uid, payload = json.encodeToString(result)) }
        } else {
            queueOperation("CREATE_PARTICIPANT", result.uid, payload = json.encodeToString(result))
        }
        return result
    }

    override suspend fun updateParticipant(participant: Participant) {
        roomRepository.updateParticipant(participant)
        if (isOnline) {
            try { firebaseRepository.updateParticipant(participant) }
            catch (e: Exception) { queueOperation("UPDATE_PARTICIPANT", participant.uid, payload = json.encodeToString(participant)) }
        } else {
            queueOperation("UPDATE_PARTICIPANT", participant.uid, payload = json.encodeToString(participant))
        }
    }

    override suspend fun deleteParticipant(participantId: String) {
        roomRepository.deleteParticipant(participantId)
        if (isOnline) {
            try { firebaseRepository.deleteParticipant(participantId) }
            catch (e: Exception) { queueOperation("DELETE_PARTICIPANT", participantId) }
        } else {
            queueOperation("DELETE_PARTICIPANT", participantId)
        }
    }

    override suspend fun getParticipantById(participantId: String): Participant? {
        if (isOnline) {
            try { return firebaseRepository.getParticipantById(participantId) }
            catch (_: Exception) {}
        }
        return roomRepository.getParticipantById(participantId)
    }

    override suspend fun findParticipantByName(firstName: String, lastName: String): Participant? {
        if (isOnline) {
            try { return firebaseRepository.findParticipantByName(firstName, lastName) }
            catch (_: Exception) {}
        }
        return roomRepository.findParticipantByName(firstName, lastName)
    }

    override suspend fun updateParticipantTime(eventId: String, participant: ParticipantTime) {
        roomRepository.updateParticipantTime(eventId, participant)
        if (isOnline) {
            try { firebaseRepository.updateParticipantTime(eventId, participant) }
            catch (e: Exception) { queueOperation("UPDATE_PARTICIPANT_TIME", participant.uid, parentId = eventId, payload = json.encodeToString(participant)) }
        } else {
            queueOperation("UPDATE_PARTICIPANT_TIME", participant.uid, parentId = eventId, payload = json.encodeToString(participant))
        }
    }

    // --- Recipes ---

    override suspend fun getAllRecipes(): List<Recipe> {
        if (isOnline) {
            try {
                val recipes = firebaseRepository.getAllRecipes()
                recipes.filter { it.uid.isNotBlank() }
                    .onEach { it.shoppingIngredients.forEach { si -> si.ingredient = null } }
                    .forEach { db.recipeDao().insert(it) }
                return recipes
            } catch (_: Exception) {}
        }
        return roomRepository.getAllRecipes()
    }

    override suspend fun getUserCreatedRecipes(): List<Recipe> {
        if (isOnline) {
            try { return firebaseRepository.getUserCreatedRecipes() }
            catch (_: Exception) {}
        }
        return roomRepository.getUserCreatedRecipes()
    }

    override suspend fun getRecipeById(recipeId: String): Recipe? {
        if (isOnline) {
            try { return firebaseRepository.getRecipeById(recipeId) }
            catch (_: Exception) {}
        }
        return roomRepository.getRecipeById(recipeId)
    }

    override suspend fun createRecipe(recipe: Recipe) {
        roomRepository.createRecipe(recipe)
        if (isOnline) {
            try { firebaseRepository.createRecipe(recipe) }
            catch (e: Exception) { queueOperation("CREATE_RECIPE", recipe.uid, payload = json.encodeToString(recipe)) }
        } else {
            queueOperation("CREATE_RECIPE", recipe.uid, payload = json.encodeToString(recipe))
        }
    }

    override suspend fun updateRecipe(recipe: Recipe) {
        roomRepository.updateRecipe(recipe)
        if (isOnline) {
            try { firebaseRepository.updateRecipe(recipe) }
            catch (e: Exception) { queueOperation("UPDATE_RECIPE", recipe.uid, payload = json.encodeToString(recipe)) }
        } else {
            queueOperation("UPDATE_RECIPE", recipe.uid, payload = json.encodeToString(recipe))
        }
    }

    override suspend fun deleteRecipe(recipeId: String) {
        roomRepository.deleteRecipe(recipeId)
        if (isOnline) {
            try { firebaseRepository.deleteRecipe(recipeId) }
            catch (e: Exception) { queueOperation("DELETE_RECIPE", recipeId) }
        } else {
            queueOperation("DELETE_RECIPE", recipeId)
        }
    }

    // --- Meals ---

    override suspend fun getAllMealsOfEvent(eventId: String): List<Meal> {
        if (isOnline) {
            try { return firebaseRepository.getAllMealsOfEvent(eventId) }
            catch (_: Exception) {}
        }
        return roomRepository.getAllMealsOfEvent(eventId)
    }

    override suspend fun getMealById(eventId: String, mealId: String): Meal {
        if (isOnline) {
            try { return firebaseRepository.getMealById(eventId, mealId) }
            catch (_: Exception) {}
        }
        return roomRepository.getMealById(eventId, mealId)
    }

    override suspend fun createNewMeal(eventId: String, day: Instant): Meal {
        val meal = roomRepository.createNewMeal(eventId, day)
        if (isOnline) {
            try { firebaseRepository.updateMeal(eventId, meal) }
            catch (e: Exception) { queueOperation("CREATE_MEAL", meal.uid, parentId = eventId, payload = json.encodeToString(meal)) }
        } else {
            queueOperation("CREATE_MEAL", meal.uid, parentId = eventId, payload = json.encodeToString(meal))
        }
        return meal
    }

    override suspend fun deleteMeal(eventId: String, mealId: String) {
        roomRepository.deleteMeal(eventId, mealId)
        if (isOnline) {
            try { firebaseRepository.deleteMeal(eventId, mealId) }
            catch (e: Exception) { queueOperation("DELETE_MEAL", mealId, parentId = eventId) }
        } else {
            queueOperation("DELETE_MEAL", mealId, parentId = eventId)
        }
    }

    override suspend fun updateMeal(eventId: String, meal: Meal) {
        roomRepository.updateMeal(eventId, meal)
        if (isOnline) {
            try { firebaseRepository.updateMeal(eventId, meal) }
            catch (e: Exception) { queueOperation("UPDATE_MEAL", meal.uid, parentId = eventId, payload = json.encodeToString(meal)) }
        } else {
            queueOperation("UPDATE_MEAL", meal.uid, parentId = eventId, payload = json.encodeToString(meal))
        }
    }

    // --- Ingredients ---

    override suspend fun getIngredientById(ingredientId: String): Ingredient {
        if (isOnline) {
            try { return firebaseRepository.getIngredientById(ingredientId) }
            catch (_: Exception) {}
        }
        return roomRepository.getIngredientById(ingredientId)
    }

    override suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal> {
        if (isOnline) {
            try { return firebaseRepository.getMealsWithRecipeAndIngredients(eventId) }
            catch (_: Exception) {}
        }
        return roomRepository.getMealsWithRecipeAndIngredients(eventId)
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        if (isOnline) {
            try {
                val ingredients = firebaseRepository.getAllIngredients()
                ingredients.filter { it.uid.isNotBlank() }.forEach { db.ingredientDao().insert(it) }
                return ingredients
            } catch (_: Exception) {}
        }
        return roomRepository.getAllIngredients()
    }

    // --- Shopping Lists ---

    override suspend fun getMultiDayShoppingList(eventId: String): MultiDayShoppingList? {
        if (isOnline) {
            try {
                val list = firebaseRepository.getMultiDayShoppingList(eventId)
                if (list != null) db.multiDayShoppingListDao().insert(list)
                return list
            } catch (_: Exception) {}
        }
        return roomRepository.getMultiDayShoppingList(eventId)
    }

    override suspend fun saveMultiDayShoppingList(eventId: String, multiDayShoppingList: MultiDayShoppingList) {
        roomRepository.saveMultiDayShoppingList(eventId, multiDayShoppingList)
        if (isOnline) {
            try { firebaseRepository.saveMultiDayShoppingList(eventId, multiDayShoppingList) }
            catch (e: Exception) { queueOperation("SAVE_MULTI_DAY_SHOPPING_LIST", eventId, parentId = eventId, payload = json.encodeToString(multiDayShoppingList)) }
        } else {
            queueOperation("SAVE_MULTI_DAY_SHOPPING_LIST", eventId, parentId = eventId, payload = json.encodeToString(multiDayShoppingList))
        }
    }

    override suspend fun getDailyShoppingList(eventId: String, date: LocalDate): DailyShoppingList? {
        val multiDayList = getMultiDayShoppingList(eventId)
        return multiDayList?.dailyLists?.get(date)
    }

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
        if (isOnline) {
            try { firebaseRepository.saveMaterialList(eventId, materialList) }
            catch (e: Exception) { queueOperation("SAVE_MATERIAL_LIST", eventId, parentId = eventId, payload = json.encodeToString(materialList)) }
        } else {
            queueOperation("SAVE_MATERIAL_LIST", eventId, parentId = eventId, payload = json.encodeToString(materialList))
        }
    }

    override suspend fun getMaterialListOfEvent(eventId: String): List<Material> {
        if (isOnline) {
            try { return firebaseRepository.getMaterialListOfEvent(eventId) }
            catch (_: Exception) {}
        }
        return roomRepository.getMaterialListOfEvent(eventId)
    }

    override suspend fun deleteMaterialById(eventId: String, materialId: String) {
        roomRepository.deleteMaterialById(eventId, materialId)
        if (isOnline) {
            try { firebaseRepository.deleteMaterialById(eventId, materialId) }
            catch (e: Exception) { queueOperation("DELETE_MATERIAL", materialId, parentId = eventId) }
        } else {
            queueOperation("DELETE_MATERIAL", materialId, parentId = eventId)
        }
    }

    override suspend fun getAllMaterials(): List<Material> {
        if (isOnline) {
            try { return firebaseRepository.getAllMaterials() }
            catch (_: Exception) {}
        }
        return roomRepository.getAllMaterials()
    }
}
