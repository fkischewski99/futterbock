package data.sync

import co.touchlab.kermit.Logger
import data.EventRepository
import data.FireBaseRepository
import data.local.AppDatabase
import data.local.RoomRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
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

    override suspend fun getEventList(group: String): Flow<List<Event>> {
        if (isOnline) {
            try {
                return firebaseRepository.getEventList(group).onEach { events ->
                    events.forEach { event ->
                        db.eventDao().insert(event)
                    }
                }
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
            try {
                val participantTimes = firebaseRepository.getParticipantsOfEvent(eventId, withParticipant)
                participantTimes.forEach { pt ->
                    pt.eventId = eventId
                    db.participantTimeDao().insert(pt)
                    if (pt.participant != null) {
                        db.participantDao().insert(pt.participant!!)
                    }
                }
                return participantTimes
            } catch (_: Exception) {}
        }
        return roomRepository.getParticipantsOfEvent(eventId, withParticipant)
    }

    override suspend fun getAllParticipantsOfStamm(): Flow<List<Participant>> {
        if (isOnline) {
            try {
                return firebaseRepository.getAllParticipantsOfStamm().onEach { participants ->
                    participants.forEach { db.participantDao().insert(it) }
                }
            } catch (_: Exception) {}
        }
        return roomRepository.getAllParticipantsOfStamm()
    }

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

    override suspend fun getParticipantById(participantId: String): Participant? {
        if (isOnline) {
            try {
                val participant = firebaseRepository.getParticipantById(participantId)
                if (participant != null) db.participantDao().insert(participant)
                return participant
            } catch (_: Exception) {}
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
        writeToFirebaseOrQueue("UPDATE_PARTICIPANT_TIME", participant.uid, parentId = eventId, payload = json.encodeToString(participant)) {
            firebaseRepository.updateParticipantTime(eventId, participant)
        }
    }

    // --- Recipes ---

    override suspend fun getAllRecipes(): List<Recipe> {
        if (isOnline) {
            try {
                val recipes = firebaseRepository.getAllRecipes()
                recipes
                    .filter { it.uid.isNotBlank() }
                    .onEach { it.shoppingIngredients.forEach { si -> si.ingredient = null } }
                    .forEach { db.recipeDao().insert(it) }
                return recipes
            } catch (_: Exception) {}
        }
        return roomRepository.getAllRecipes()
    }

    override suspend fun getUserCreatedRecipes(): List<Recipe> {
        if (isOnline) {
            try {
                val recipes = firebaseRepository.getUserCreatedRecipes()
                recipes
                    .filter { it.uid.isNotBlank() }
                    .onEach { it.shoppingIngredients.forEach { si -> si.ingredient = null } }
                    .forEach { db.recipeDao().insert(it) }
                return recipes
            } catch (_: Exception) {}
        }
        return roomRepository.getUserCreatedRecipes()
    }

    override suspend fun getRecipeById(recipeId: String): Recipe? {
        if (isOnline) {
            try {
                val recipe = firebaseRepository.getRecipeById(recipeId)
                if (recipe != null && recipe.uid.isNotBlank()) {
                    val toCache = Recipe().apply {
                        uid = recipe.uid; name = recipe.name; description = recipe.description
                        cookingInstructions = recipe.cookingInstructions; notes = recipe.notes
                        dietaryHabit = recipe.dietaryHabit; materials = recipe.materials
                        pageInCookbook = recipe.pageInCookbook; price = recipe.price
                        season = recipe.season; foodIntolerances = recipe.foodIntolerances
                        source = recipe.source; time = recipe.time; skillLevel = recipe.skillLevel
                        type = recipe.type
                        shoppingIngredients = recipe.shoppingIngredients.map { si ->
                            ShoppingIngredient().apply {
                                uid = si.uid; ingredientRef = si.ingredientRef
                                nameEnteredByUser = si.nameEnteredByUser; amount = si.amount
                                unit = si.unit; title = si.title; shoppingDone = si.shoppingDone
                                note = si.note; source = si.source
                            }
                        }
                    }
                    db.recipeDao().insert(toCache)
                }
                return recipe
            } catch (_: Exception) {}
        }
        return roomRepository.getRecipeById(recipeId)
    }

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

    override suspend fun getAllMealsOfEvent(eventId: String): List<Meal> {
        if (isOnline) {
            try {
                val meals = firebaseRepository.getAllMealsOfEvent(eventId)
                meals.forEach { meal ->
                    meal.eventId = eventId
                    db.mealDao().insert(meal)
                }
                return meals
            } catch (_: Exception) {}
        }
        return roomRepository.getAllMealsOfEvent(eventId)
    }

    override suspend fun getMealById(eventId: String, mealId: String): Meal {
        if (isOnline) {
            try {
                val meal = firebaseRepository.getMealById(eventId, mealId)
                meal.eventId = eventId
                db.mealDao().insert(meal)
                return meal
            } catch (_: Exception) {}
        }
        return roomRepository.getMealById(eventId, mealId)
    }

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

    override suspend fun getIngredientById(ingredientId: String): Ingredient {
        if (isOnline) {
            try {
                val ingredient = firebaseRepository.getIngredientById(ingredientId)
                db.ingredientDao().insert(ingredient)
                return ingredient
            } catch (_: Exception) {}
        }
        return roomRepository.getIngredientById(ingredientId)
    }

    override suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal> {
        if (isOnline) {
            try {
                val meals = firebaseRepository.getMealsWithRecipeAndIngredients(eventId)
                meals.forEach { meal ->
                    meal.eventId = eventId
                    db.mealDao().insert(meal)
                }
                return meals
            } catch (_: Exception) {}
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
        writeToFirebaseOrQueue("SAVE_MULTI_DAY_SHOPPING_LIST", eventId, parentId = eventId, payload = json.encodeToString(multiDayShoppingList)) {
            firebaseRepository.saveMultiDayShoppingList(eventId, multiDayShoppingList)
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
        writeToFirebaseOrQueue("SAVE_MATERIAL_LIST", eventId, parentId = eventId, payload = json.encodeToString(materialList)) {
            firebaseRepository.saveMaterialList(eventId, materialList)
        }
    }

    override suspend fun getMaterialListOfEvent(eventId: String): List<Material> {
        if (isOnline) {
            try {
                val materials = firebaseRepository.getMaterialListOfEvent(eventId)
                materials.forEach { db.materialDao().insert(it) }
                return materials
            } catch (_: Exception) {}
        }
        return roomRepository.getMaterialListOfEvent(eventId)
    }

    override suspend fun deleteMaterialById(eventId: String, materialId: String) {
        roomRepository.deleteMaterialById(eventId, materialId)
        writeToFirebaseOrQueue("DELETE_MATERIAL", materialId, parentId = eventId) {
            firebaseRepository.deleteMaterialById(eventId, materialId)
        }
    }

    override suspend fun getAllMaterials(): List<Material> {
        if (isOnline) {
            try {
                val materials = firebaseRepository.getAllMaterials()
                materials.filter { it.uid.isNotBlank() }.forEach { db.materialDao().insert(it) }
                return materials
            } catch (_: Exception) {}
        }
        return roomRepository.getAllMaterials()
    }
}
