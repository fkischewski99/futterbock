package data.local

import co.touchlab.kermit.Logger
import data.EventRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import model.*
import services.login.LoginAndRegister
import view.shared.HelperFunctions.Companion.generateRandomStringId
import kotlin.time.Duration.Companion.days

class RoomRepository(
    private val db: AppDatabase,
    private val loginAndRegister: LoginAndRegister
) : EventRepository {

    // --- Event ---

    override suspend fun deleteEvent(eventId: String) {
        db.eventDao().deleteById(eventId)
    }

    override suspend fun getEventById(eventId: String): Event? {
        return db.eventDao().getById(eventId)
    }

    override suspend fun createNewEvent(): Event {
        val eventId = generateRandomStringId()
        val userGroup = loginAndRegister.getCustomUserGroup()
        val event = Event(userGroup).apply {
            uid = eventId
            name = "Neues Lager"
            from = Clock.System.now()
            to = Clock.System.now().plus(2.days)
        }
        db.eventDao().insert(event)
        return event
    }

    override suspend fun saveExistingEvent(event: Event) {
        db.eventDao().update(event)
    }

    override suspend fun getEventList(group: String): Flow<List<Event>> {
        return db.eventDao().getByGroup(group)
    }

    // --- Participants ---

    override suspend fun getNumberOfParticipants(eventId: String): Int {
        return db.participantTimeDao().getCountByEventId(eventId)
    }

    override suspend fun getParticipantsOfEvent(
        eventId: String,
        withParticipant: Boolean
    ): List<ParticipantTime> {
        val participantTimes = db.participantTimeDao().getByEventId(eventId)

        if (!withParticipant || participantTimes.isEmpty()) {
            return participantTimes
        }

        val validParticipantTimes = mutableListOf<ParticipantTime>()
        val orphanedIds = mutableListOf<String>()

        participantTimes.forEach { pt ->
            val participant = db.participantDao().getById(pt.participantRef)
            if (participant != null) {
                pt.participant = participant
                validParticipantTimes.add(pt)
            } else {
                orphanedIds.add(pt.participantRef)
            }
        }

        orphanedIds.forEach { id ->
            db.participantTimeDao().deleteByEventAndParticipant(eventId, id)
        }

        return validParticipantTimes
    }

    override suspend fun getAllParticipantsOfStamm(): Flow<List<Participant>> {
        val group = loginAndRegister.getCustomUserGroup()
        return db.participantDao().getByGroup(group)
    }

    override suspend fun deleteParticipantOfEvent(eventId: String, participantId: String) {
        db.participantTimeDao().deleteByEventAndParticipant(eventId, participantId)
    }

    override suspend fun addParticipantToEvent(
        newParticipant: Participant,
        event: Event
    ): ParticipantTime {
        val participantTimeId = generateRandomStringId()
        val participantTime = ParticipantTime(
            uid = participantTimeId,
            from = event.from,
            to = event.to,
            participantRef = newParticipant.uid,
            cookingGroup = newParticipant.selectedGroup.takeIf { it.isNotBlank() } ?: ""
        ).also {
            it.participant = newParticipant
            it.eventId = event.uid
        }
        db.participantTimeDao().insert(participantTime)
        return participantTime
    }

    override suspend fun createNewParticipant(participant: Participant): Participant? {
        try {
            val existing = findParticipantByName(participant.firstName, participant.lastName)
            if (existing != null) {
                Logger.w("Participant with name '${participant.firstName} ${participant.lastName}' already exists.")
                return null
            }

            val participantId = generateRandomStringId()
            participant.uid = participantId
            participant.group = loginAndRegister.getCustomUserGroup()
            db.participantDao().insert(participant)
            return participant
        } catch (e: Exception) {
            Logger.e("Error creating participant: ${participant.firstName} ${participant.lastName}", e)
            return null
        }
    }

    override suspend fun updateParticipant(participant: Participant) {
        participant.group = loginAndRegister.getCustomUserGroup()
        db.participantDao().update(participant)
    }

    override suspend fun deleteParticipant(participantId: String) {
        val userGroup = loginAndRegister.getCustomUserGroup()
        val events = db.eventDao().getByGroup(userGroup).first()

        events.forEach { event ->
            db.mealDao().getByEventId(event.uid)
                .filter { meal -> meal.recipeSelections.any { it.eaterIds.contains(participantId) } }
                .forEach { meal ->
                    meal.recipeSelections.forEach { it.eaterIds.remove(participantId) }
                    db.mealDao().update(meal)
                }
        }

        db.participantDao().deleteById(participantId)
    }

    override suspend fun getParticipantById(participantId: String): Participant? {
        return db.participantDao().getById(participantId)
    }

    override suspend fun findParticipantByName(firstName: String, lastName: String): Participant? {
        return db.participantDao().findByName(firstName.trim(), lastName.trim())
    }

    override suspend fun updateParticipantTime(eventId: String, participant: ParticipantTime) {
        participant.eventId = eventId
        db.participantTimeDao().update(participant)
    }

    // --- Recipes ---

    override suspend fun getAllRecipes(): List<Recipe> {
        return db.recipeDao().getAll()
    }

    override suspend fun getUserCreatedRecipes(): List<Recipe> {
        val userGroup = loginAndRegister.getCustomUserGroup()
        return db.recipeDao().getBySource(userGroup)
    }

    override suspend fun getRecipeById(recipeId: String): Recipe? {
        val recipe = db.recipeDao().getById(recipeId) ?: return null

        val ingredientRefs = recipe.shoppingIngredients.map { it.ingredientRef }.distinct()
        if (ingredientRefs.isNotEmpty()) {
            val ingredientMap = db.ingredientDao().getByIds(ingredientRefs).associateBy { it.uid }
            recipe.shoppingIngredients.forEach { si ->
                si.ingredient = ingredientMap[si.ingredientRef]
            }
        }

        return recipe
    }

    override suspend fun createRecipe(recipe: Recipe) {
        recipe.source = loginAndRegister.getCustomUserGroup()
        recipe.shoppingIngredients.forEach { it.ingredient = null }
        if (recipe.uid.isBlank()) {
            recipe.uid = generateRandomStringId()
        }
        db.recipeDao().insert(recipe)
    }

    override suspend fun updateRecipe(recipe: Recipe) {
        recipe.shoppingIngredients.forEach { it.ingredient = null }
        db.recipeDao().update(recipe)
    }

    override suspend fun deleteRecipe(recipeId: String) {
        db.recipeDao().deleteById(recipeId)
    }

    // --- Meals ---

    private suspend fun enrichMealsWithRecipes(meals: List<Meal>) {
        val recipeIds = meals.flatMap { it.recipeSelections.map { rs -> rs.recipeRef } }.distinct()
        if (recipeIds.isEmpty()) return

        val recipeMap = db.recipeDao().getByIds(recipeIds).associateBy { it.uid }
        val ingredientIds = recipeMap.values.flatMap { it.shoppingIngredients.map { si -> si.ingredientRef } }.distinct()
        val ingredientMap = if (ingredientIds.isNotEmpty()) db.ingredientDao().getByIds(ingredientIds).associateBy { it.uid } else emptyMap()

        meals.forEach { meal ->
            meal.recipeSelections.forEach { selection ->
                val recipe = recipeMap[selection.recipeRef]
                if (recipe != null) {
                    recipe.shoppingIngredients.forEach { si ->
                        si.ingredient = ingredientMap[si.ingredientRef]
                    }
                    selection.recipe = recipe
                }
            }
        }
    }

    override suspend fun getAllMealsOfEvent(eventId: String): List<Meal> {
        val meals = db.mealDao().getByEventId(eventId)
        enrichMealsWithRecipes(meals)
        return meals
    }

    override suspend fun getMealById(eventId: String, mealId: String): Meal {
        val meal = db.mealDao().getById(eventId, mealId)
            ?: throw NoSuchElementException("Meal $mealId not found in event $eventId")
        enrichMealsWithRecipes(listOf(meal))
        return meal
    }

    override suspend fun createNewMeal(eventId: String, day: Instant): Meal {
        val mealId = generateRandomStringId()
        val meal = Meal(day = day, uid = mealId).also { it.eventId = eventId }
        db.mealDao().insert(meal)
        return meal
    }

    override suspend fun deleteMeal(eventId: String, mealId: String) {
        db.mealDao().deleteById(eventId, mealId)
    }

    override suspend fun updateMeal(eventId: String, meal: Meal) {
        meal.eventId = eventId
        db.mealDao().update(meal)
    }

    // --- Ingredients ---

    override suspend fun getIngredientById(ingredientId: String): Ingredient {
        return db.ingredientDao().getById(ingredientId)
            ?: throw NoSuchElementException("Ingredient $ingredientId not found")
    }

    override suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal> {
        return getAllMealsOfEvent(eventId)
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        return db.ingredientDao().getAll()
    }

    // --- Shopping Lists ---

    override suspend fun getMultiDayShoppingList(eventId: String): MultiDayShoppingList? {
        return db.multiDayShoppingListDao().getByEventId(eventId)
    }

    override suspend fun saveMultiDayShoppingList(
        eventId: String,
        multiDayShoppingList: MultiDayShoppingList
    ) {
        db.multiDayShoppingListDao().insert(multiDayShoppingList)
    }

    override suspend fun getDailyShoppingList(eventId: String, date: LocalDate): DailyShoppingList? {
        val multiDayList = getMultiDayShoppingList(eventId)
        return multiDayList?.dailyLists?.get(date)
    }

    override suspend fun saveDailyShoppingList(
        eventId: String,
        date: LocalDate,
        dailyShoppingList: DailyShoppingList
    ) {
        val multiDayList = getMultiDayShoppingList(eventId)
        if (multiDayList != null) {
            val updatedDailyLists = multiDayList.dailyLists.toMutableMap()
            updatedDailyLists[date] = dailyShoppingList
            val updatedMultiDayList = multiDayList.copy(dailyLists = updatedDailyLists)
            saveMultiDayShoppingList(eventId, updatedMultiDayList)
        }
    }

    override suspend fun updateShoppingIngredientStatus(
        eventId: String,
        date: LocalDate,
        ingredientId: String,
        completed: Boolean
    ) {
        val dailyList = getDailyShoppingList(eventId, date)
        if (dailyList != null) {
            val updatedIngredients = dailyList.ingredients.map { ingredient ->
                if (ingredient.uid == ingredientId || ingredient.ingredientRef == ingredientId) {
                    ingredient.apply { shoppingDone = completed }
                } else {
                    ingredient
                }
            }
            val updatedDailyList = dailyList.copy(ingredients = updatedIngredients)
            saveDailyShoppingList(eventId, date, updatedDailyList)
        }
    }

    override suspend fun deleteShoppingListForDate(eventId: String, date: LocalDate) {
        val multiDayList = getMultiDayShoppingList(eventId)
        if (multiDayList != null) {
            val updatedDailyLists = multiDayList.dailyLists.toMutableMap()
            updatedDailyLists.remove(date)
            val updatedMultiDayList = multiDayList.copy(dailyLists = updatedDailyLists)
            saveMultiDayShoppingList(eventId, updatedMultiDayList)
        }
    }

    // --- Materials ---

    override suspend fun saveMaterialList(eventId: String, materialList: List<Material>) {
        materialList.forEach { material ->
            db.materialDao().insert(material)
        }
    }

    override suspend fun getMaterialListOfEvent(eventId: String): List<Material> {
        // Materials are stored globally; event-scoped material list uses the same table
        // For now, return all materials. Event-specific filtering can be added later.
        return db.materialDao().getAll()
    }

    override suspend fun deleteMaterialById(eventId: String, materialId: String) {
        db.materialDao().deleteById(materialId)
    }

    override suspend fun getAllMaterials(): List<Material> {
        return db.materialDao().getAll()
    }
}
