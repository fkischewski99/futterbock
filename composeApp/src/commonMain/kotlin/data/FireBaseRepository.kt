package data

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
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
import model.Source
import services.login.LoginAndRegister
import view.shared.HelperFunctions.Companion.generateRandomStringId
import kotlin.time.Duration.Companion.days

private const val EVENTS = "EVENTS"
private const val PARTICIPANT_SCHEDULE = "PARTICIPANT_SCHEDULE"
private const val MEALS = "MEALS"
private const val INGREDIENT = "INGREDIENTS"
private const val RECIPES = "RECIPE"
private const val PARTICIPANTS = "PARTICIPANTS"
private const val MULTI_DAY_SHOPPING_LIST = "MULTI_DAY_SHOPPING_LIST"
private const val MATERIAL_LIST = "MATERIAL_LIST"
private const val MATERIALS = "MATERIALS"

class FireBaseRepository(private val loginAndRegister: LoginAndRegister) : EventRepository {
    private val firestore = Firebase.firestore


    override suspend fun deleteEvent(eventId: String) {
        firestore.collection(EVENTS).document(eventId).delete()
    }

    override suspend fun getEventById(eventId: String): Event {
        val snapshot = firestore.collection(EVENTS).document(eventId)
        return snapshot.get().data<Event> { }
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
        firestore.collection(EVENTS)
            .document(eventId)
            .set(event)
        //println(eventId + " " + event.uid)
        return event;
    }

    override suspend fun saveExistingEvent(event: Event) {
        val serializableEvent = event as Event
        firestore.collection(EVENTS)
            .document(event.uid)
            .set(serializableEvent)
    }

    override suspend fun getEventList(group: String) = flow {
        firestore.collection(EVENTS).snapshots.collect { querySnapshot ->
            val userEvents = querySnapshot.documents.filter { documentSnapshot ->
                documentSnapshot.data<Event>().group == group
            }.map { documentSnapshot ->
                documentSnapshot.data<Event>()
            }
            emit(userEvents)
        }
    }

    override suspend fun getNumberOfParticipants(eventId: String): Int {
        try {
            val eventRef = firestore.collection(EVENTS)
                .document(eventId)
            return eventRef
                .collection(PARTICIPANT_SCHEDULE)
                .get()
                .documents.size
        } catch (e: IllegalArgumentException) {
            return 0;
        }
    }

    override suspend fun getMealsWithRecipeAndIngredients(eventId: String): List<Meal> {
        val mealList = getAllMealsOfEvent(eventId)

        // Collect all unique recipe IDs to batch fetch
        val allRecipeIds = mealList.flatMap { meal ->
            meal.recipeSelections.map { it.recipeRef }
        }.distinct()

        // Batch fetch all recipes if any exist
        val recipeBatch = if (allRecipeIds.isNotEmpty()) {
            getBatchRecipes(allRecipeIds)
        } else emptyMap()

        // Map recipes to selections
        mealList.forEach { meal ->
            meal.recipeSelections.forEach { selection ->
                selection.recipe = recipeBatch[selection.recipeRef]
            }
        }

        return mealList
    }

    private suspend fun getBatchRecipes(recipeIds: List<String>): Map<String, Recipe> {
        return coroutineScope {
            // Firestore 'in' query limit is 10, so we chunk the requests
            recipeIds.chunked(10).map { chunk ->
                async {
                    try {
                        firestore.collection(RECIPES)
                            .where {
                                "__name__" inArray chunk
                            }
                            .get()
                            .documents
                            .associate { doc ->
                                val recipe = doc.data<Recipe>()
                                // Populate ingredients for each recipe
                                coroutineScope {
                                    recipe.shoppingIngredients.map { shoppingIngredient ->
                                        async {
                                            shoppingIngredient.ingredient =
                                                firestore.collection(INGREDIENT)
                                                    .document(shoppingIngredient.ingredientRef)
                                                    .get()
                                                    .data<Ingredient?>()
                                        }
                                    }.awaitAll()
                                }
                                doc.id to recipe
                            }
                    } catch (e: Exception) {
                        Logger.e("Error batch fetching recipes: ${e.message}")
                        emptyMap<String, Recipe>()
                    }
                }
            }.awaitAll().fold(emptyMap<String, Recipe>()) { acc, map -> acc + map }
        }
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        return firestore.collection(INGREDIENT).get().documents.map { query ->
            query.data<Ingredient>()
        }.toCollection(ArrayList())
    }

    override suspend fun saveMaterialList(eventId: String, materialList: List<Material>) {
        coroutineScope {
            materialList.map { material ->
                async {
                    firestore.collection(EVENTS).document(eventId).collection(MATERIAL_LIST)
                        .document(material.uid).set(material)
                }
            }
        }
    }

    override suspend fun getMaterialListOfEvent(eventId: String): List<Material> {
        return firestore.collection(EVENTS).document(eventId)
            .collection(MATERIAL_LIST)
            .get().documents.map { querySnapshot ->
                querySnapshot.data { }
            }
    }

    override suspend fun deleteMaterialById(eventId: String, materialId: String) {
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(MATERIAL_LIST)
            .document(materialId)
            .delete()
    }

    override suspend fun getAllMaterials(): List<Material> {
        return firestore.collection(MATERIALS).get().documents
            .map { material -> material.data { } }
    }

    override suspend fun getAllMealsOfEvent(eventId: String): List<Meal> {
        val mealList: List<Meal> = firestore.collection(EVENTS).document(eventId)
            .collection(MEALS).get().documents
            .map { meal -> meal.data { } }

        // Batch fetch all recipes for all meals at once
        val allRecipeIds = mealList.flatMap { meal ->
            meal.recipeSelections.map { it.recipeRef }
        }.distinct()

        val recipeBatch = if (allRecipeIds.isNotEmpty()) {
            getBatchRecipes(allRecipeIds)
        } else emptyMap()

        // Map recipes to all meals
        mealList.forEach { meal ->
            meal.recipeSelections.forEach { selection ->
                selection.recipe = recipeBatch[selection.recipeRef]
            }
        }

        return mealList
    }

    override suspend fun getMealById(eventId: String, mealId: String): Meal {
        val snapshot = firestore.collection(EVENTS).document(eventId)
            .collection(MEALS).document(mealId)
        val meal = snapshot.get().data<Meal>()

        // Use batch recipe fetching for consistency and performance
        val recipeIds = meal.recipeSelections.map { it.recipeRef }.distinct()
        val recipeBatch = if (recipeIds.isNotEmpty()) {
            getBatchRecipes(recipeIds)
        } else emptyMap()

        meal.recipeSelections.forEach { recipeSelection ->
            recipeSelection.recipe = recipeBatch[recipeSelection.recipeRef]
        }

        return meal
    }

    override suspend fun getParticipantsOfEvent(
        eventId: String,
        withParticipant: Boolean
    ): List<ParticipantTime> {
        // Remove unnecessary existence check - just fetch participant schedule directly
        val allParticipantTime = try {
            firestore.collection(EVENTS).document(eventId)
                .collection(PARTICIPANT_SCHEDULE)
                .get().documents.map<DocumentSnapshot, ParticipantTime> { querySnapshot ->
                    querySnapshot.data { }
                }
        } catch (e: Exception) {
            Logger.e("Error fetching participant schedule: ${e.message}")
            return emptyList()
        }

        if (!withParticipant || allParticipantTime.isEmpty()) {
            return allParticipantTime
        }

        // Batch fetch all participants
        val participantIds = allParticipantTime.map { it.participantRef }.distinct()
        val participantMap = getBatchParticipants(participantIds)

        // Clean up missing participants and map results
        val validParticipantTimes = mutableListOf<ParticipantTime>()
        val participantsToDelete = mutableListOf<String>()

        allParticipantTime.forEach { participantTime ->
            val participant = participantMap[participantTime.participantRef]
            if (participant == null) {
                participantsToDelete.add(participantTime.participantRef)
            } else {
                participantTime.participant = participant
                validParticipantTimes.add(participantTime)
            }
        }

        // Cleanup orphaned participant references in background
        if (participantsToDelete.isNotEmpty()) {
            coroutineScope {
                participantsToDelete.map { participantId ->
                    async {
                        try {
                            deleteParticipantOfEvent(eventId, participantId)
                        } catch (e: Exception) {
                            Logger.e("Error deleting orphaned participant $participantId: ${e.message}")
                        }
                    }
                }
            }
        }

        return validParticipantTimes
    }

    private suspend fun getBatchParticipants(participantIds: List<String>): Map<String, Participant> {
        if (participantIds.isEmpty()) return emptyMap()

        return coroutineScope {
            val group = loginAndRegister.getCustomUserGroup()
            participantIds.chunked(10).map { chunk ->
                async {
                    try {
                        firestore.collection(PARTICIPANTS)
                            .where {
                                "__name__" inArray chunk
                                "group" equalTo group
                            }
                            .get()
                            .documents
                            .associate { doc -> doc.id to doc.data<Participant>() }
                    } catch (e: Exception) {
                        Logger.e("Error batch fetching participants: ${e.message}")
                        emptyMap<String, Participant>()
                    }
                }
            }.awaitAll().fold(emptyMap<String, Participant>()) { acc, map -> acc + map }
        }
    }

    private suspend fun getBatchIngredients(ingredientIds: List<String>): List<Ingredient> {
        if (ingredientIds.isEmpty()) return emptyList()

        return coroutineScope {
            ingredientIds.chunked(10).map { chunk ->
                async {
                    try {
                        firestore.collection(INGREDIENT)
                            .where {
                                "__name__" inArray chunk
                            }
                            .get()
                            .documents
                            .map { doc -> doc.data<Ingredient> { } }
                    } catch (e: Exception) {
                        Logger.e("Error fetching ingredient batch: ${e.message}")
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }

    override suspend fun getAllParticipantsOfStamm() = flow {
        firestore.collection(PARTICIPANTS).snapshots.collect { querySnapshot ->
            val userEvents = querySnapshot.documents.filter { documentSnapshot ->
                documentSnapshot.data<Participant>().group == loginAndRegister.getCustomUserGroup()
            }.map { documentSnapshot ->
                documentSnapshot.data<Participant>()
            }
            emit(userEvents)
        }
    }

    override suspend fun getParticipantById(participantId: String): Participant? {
        return firestore.collection(PARTICIPANTS).document(participantId).get().data { }
    }

    override suspend fun getAllRecipes(): List<Recipe> {
        val recipes: MutableList<Recipe> = mutableListOf()
        firestore.collection(RECIPES).get().documents.map { query ->
            val recipe = query.data<Recipe>()
            recipes.add(recipe)
            Logger.i("Deserialized: " + recipe.name)
        }
        return recipes
    }

    override suspend fun getRecipeById(recipeId: String): Recipe {
        val recipe = firestore
            .collection(RECIPES)
            .document(recipeId)
            .get()
            .data<Recipe> {
            }

        // Batch load all ingredients in a single request instead of individual requests
        val ingredientRefs = recipe.shoppingIngredients.map { it.ingredientRef }.distinct()
        if (ingredientRefs.isNotEmpty()) {
            val ingredients = getBatchIngredients(ingredientRefs)
            val ingredientMap = ingredients.associateBy { it.uid }

            // Map ingredients to shopping ingredients
            recipe.shoppingIngredients.forEach { shoppingIngredient ->
                shoppingIngredient.ingredient = ingredientMap[shoppingIngredient.ingredientRef]
            }
        }

        return recipe
    }

    override suspend fun getIngredientById(ingredientId: String): Ingredient {
        return firestore.collection(INGREDIENT).document(ingredientId).get().data { }
    }

    override suspend fun createNewMeal(eventId: String, day: Instant): Meal {
        val mealId = generateRandomStringId()
        val meal = Meal(day = day, uid = mealId)
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(MEALS)
            .document(meal.uid)
            .set(meal)
        return meal
    }

    override suspend fun deleteMeal(eventId: String, mealId: String) {
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(MEALS)
            .document(mealId)
            .delete()
    }

    override suspend fun updateMeal(eventId: String, meal: Meal) {
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(MEALS)
            .document(meal.uid)
            .set(meal)
    }

    override suspend fun updateParticipantTime(eventId: String, participant: ParticipantTime) {
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(PARTICIPANT_SCHEDULE)
            .document(participant.uid)
            .set(participant)
    }

    override suspend fun findParticipantByName(firstName: String, lastName: String): Participant? {
        return try {
            Logger.i("Find Participant by Name '$firstName' '$lastName'")
            val userGroup = loginAndRegister.getCustomUserGroup()
            Logger.i("Query parameters: firstName='${firstName.trim()}', lastName='${lastName.trim()}', group='$userGroup'")

            val documents = firestore.collection(PARTICIPANTS)
                .where {
                    ("firstName" equalTo firstName.trim()) and
                            ("lastName" equalTo lastName.trim()) and
                            ("group" equalTo userGroup)
                }
                .limit(10)
                .get()
                .documents

            Logger.i("Query returned ${documents.size} documents")
            documents.forEach { document ->
                val participant = document.data<Participant>()
                Logger.i("Found Participant: firstName='${participant.firstName}', lastName='${participant.lastName}', id='${participant.uid}'")
            }

            return documents.firstOrNull()?.data<Participant>()
        } catch (e: Exception) {
            Logger.e("Error finding participant by name: $firstName $lastName", e)
            null
        }
    }

    override suspend fun createNewParticipant(participant: Participant): Participant? {
        try {
            val existing = findParticipantByName(participant.firstName, participant.lastName)

            if (existing != null) {
                Logger.w("Participant with name '${participant.firstName} ${participant.lastName}' already exists. Skipping import.")
                return null
            }

            Logger.i("Create New Participant")
            val participantId = generateRandomStringId()
            participant.uid = participantId
            participant.group = loginAndRegister.getCustomUserGroup()
            firestore.collection(PARTICIPANTS)
                .document(participant.uid)
                .set(participant)
            return participant
        } catch (e: Exception) {
            Logger.e(
                "Error creating participant: ${participant.firstName} ${participant.lastName}",
                e
            )
            return null
        }
    }

    override suspend fun updateParticipant(participant: Participant) {
        participant.group = loginAndRegister.getCustomUserGroup()
        firestore.collection(PARTICIPANTS).document(participant.uid).set(participant)
    }

    override suspend fun deleteParticipant(participantId: String) {
        // Delete participant from all meals and participant schedules across all events in user's group
        coroutineScope {
            try {
                val userGroup = loginAndRegister.getCustomUserGroup()

                // Get events from user's group only
                val events = firestore.collection(EVENTS)
                    .where { "group" equalTo userGroup }
                    .get()
                    .documents

                // Only process events where the participant is in the participant schedule
                val eventsWithParticipant = events.mapNotNull { eventDoc ->
                    async {
                        val eventId = eventDoc.id
                        
                        // Check if participant exists in participant schedule for this event
                        val participantScheduleExists = try {
                            firestore.collection(EVENTS)
                                .document(eventId)
                                .collection(PARTICIPANT_SCHEDULE)
                                .document(participantId)
                                .get()
                                .exists
                        } catch (e: Exception) {
                            Logger.w("Error checking participant schedule existence for participant $participantId in event $eventId: ${e.message}")
                            false
                        }

                        if (participantScheduleExists) eventId else null
                    }
                }.awaitAll().filterNotNull()

                // Batch process meals only for events where participant is scheduled
                val batchUpdates = eventsWithParticipant.map { eventId ->
                    async {
                        // Get all meals for this event in one query
                        val meals = firestore.collection(EVENTS)
                            .document(eventId)
                            .collection(MEALS)
                            .get()
                            .documents
                            .map { it.data<Meal>() }

                        // Filter and update only meals that contain the participant
                        meals.mapNotNull { meal ->
                            var mealUpdated = false

                            // Remove participant from all recipe selections
                            meal.recipeSelections.forEach { recipeSelection ->
                                if (recipeSelection.eaterIds.contains(participantId)) {
                                    recipeSelection.eaterIds.remove(participantId)
                                    mealUpdated = true
                                }
                            }

                            // Return meal for batch update if modified
                            if (mealUpdated) {
                                eventId to meal
                            } else {
                                null
                            }
                        }
                    }
                }.awaitAll().flatten()

                // Perform batch updates for meals
                batchUpdates.map { (eventId, meal) ->
                    async {
                        firestore.collection(EVENTS)
                            .document(eventId)
                            .collection(MEALS)
                            .document(meal.uid)
                            .set(meal)

                        Logger.d("Removed participant $participantId from meal ${meal.uid} in event $eventId")
                    }
                }.awaitAll()

                // Remove participant from participant schedules
                eventsWithParticipant.map { eventId ->
                    async {
                        firestore.collection(EVENTS)
                            .document(eventId)
                            .collection(PARTICIPANT_SCHEDULE)
                            .document(participantId)
                            .delete()

                        Logger.d("Removed participant $participantId from participant schedule in event $eventId")
                    }
                }.awaitAll()

                Logger.i("Successfully removed participant $participantId from ${batchUpdates.size} meals and ${eventsWithParticipant.size} participant schedules in user group")

            } catch (e: Exception) {
                Logger.e("Error removing participant $participantId from meals and schedules: ${e.message}")
            }
        }

        // Delete the participant document
        firestore.collection(PARTICIPANTS).document(participantId).delete()
    }

    override suspend fun deleteParticipantOfEvent(eventId: String, participantId: String) {
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(PARTICIPANT_SCHEDULE)
            .document(participantId)
            .delete()
    }

    override suspend fun addParticipantToEvent(
        newParticipant: Participant,
        event: Event
    ): ParticipantTime {
        val participantId = generateRandomStringId()
        val participant = ParticipantTime(
            participant = newParticipant,
            from = event.from,
            to = event.to,
            uid = participantId,
            participantRef = newParticipant.uid,
            cookingGroup = newParticipant.selectedGroup.takeIf { it.isNotBlank() } ?: ""
        )
        firestore.collection(EVENTS)
            .document(event.uid)
            .collection(PARTICIPANT_SCHEDULE)
            .document(participantId)
            .set(participant)
        return participant
    }

    // Multi-day shopping list methods
    override suspend fun getMultiDayShoppingList(eventId: String): MultiDayShoppingList? {
        return try {
            val snapshot = firestore.collection(EVENTS)
                .document(eventId)
                .collection(MULTI_DAY_SHOPPING_LIST)
                .document("multiDayList")
                .get()

            if (snapshot.exists) {
                snapshot.data<MultiDayShoppingList>()
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.e("Error getting multi-day shopping list: ${e.message}")
            null
        }
    }

    override suspend fun saveMultiDayShoppingList(
        eventId: String,
        multiDayShoppingList: MultiDayShoppingList
    ) {
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(MULTI_DAY_SHOPPING_LIST)
            .document("multiDayList")
            .set(multiDayShoppingList)
    }

    override suspend fun getDailyShoppingList(
        eventId: String,
        date: LocalDate
    ): DailyShoppingList? {
        return try {
            val multiDayList = getMultiDayShoppingList(eventId)
            multiDayList?.dailyLists?.get(date)
        } catch (e: Exception) {
            Logger.e("Error getting daily shopping list: ${e.message}")
            null
        }
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
}