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
import model.Event
import model.Ingredient
import model.Material
import model.Meal
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
private const val SHOPPING_LIST = "SHOPPING_LIST"
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

        coroutineScope {
            mealList.map { meal ->
                async {
                    meal.recipeSelections.map { recipeSelection ->
                        async {
                            val recipe = getRecipeById(recipeSelection.recipeRef)
                            recipeSelection.recipe = recipe
                            recipeSelection
                        }
                    }.awaitAll()
                    meal
                }
            }.awaitAll()
        }
        return mealList

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

    override suspend fun deleteShoppingListItemById(eventId: String, listItemId: String) {
        firestore.collection(EVENTS)
            .document(eventId)
            .collection(SHOPPING_LIST)
            .document(listItemId)
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
        val mealListWithRecipes = mealList.map { meal -> getMealById(eventId, meal.uid) }
        return mealListWithRecipes
    }

    override suspend fun getMealById(eventId: String, mealId: String): Meal {
        val snapshot = firestore.collection(EVENTS).document(eventId)
            .collection(MEALS).document(mealId)
        val meal = snapshot.get().data<Meal>()
        coroutineScope {
            meal.recipeSelections.map { recipeSelection ->
                async {
                    val recipe =
                        firestore.collection(RECIPES).document(recipeSelection.recipeRef).get()
                            .data<Recipe> { }
                    recipeSelection.recipe = recipe
                }
            }.awaitAll()
        }
        return meal
    }

    override suspend fun getShoppingIngredients(eventId: String): List<ShoppingIngredient> {
        return firestore.collection(EVENTS).document(eventId)
            .collection(SHOPPING_LIST)
            .get().documents.map { querySnapshot ->
                querySnapshot.data { }
            }
    }

    override suspend fun saveShoppingList(eventId: String, shoppingList: List<ShoppingIngredient>) {
        coroutineScope {
            shoppingList.map { shoppingIngredient ->
                async {
                    if (shoppingIngredient.source == Source.COMPUTED) {
                        firestore.collection(EVENTS).document(eventId).collection(SHOPPING_LIST)
                            .document(shoppingIngredient.ingredientRef).set(shoppingIngredient)
                    }
                    if (shoppingIngredient.source == Source.ENTERED_BY_USER) {
                        firestore.collection(EVENTS).document(eventId).collection(SHOPPING_LIST)
                            .document(shoppingIngredient.uid).set(shoppingIngredient)
                    }
                }
            }
        }
    }

    override suspend fun getParticipantsOfEvent(
        eventId: String,
        withParticipant: Boolean
    ): List<ParticipantTime> {
        val mealsExist = firestore.collection(EVENTS).document(eventId).get().exists

        if (!mealsExist) {
            return emptyList()
        }
        val allParticipantTime = firestore.collection(EVENTS).document(eventId)
            .collection(PARTICIPANT_SCHEDULE)
            .get().documents.map<DocumentSnapshot, ParticipantTime> { querySnapshot ->
                querySnapshot.data { }
            }
        if (!withParticipant)
            return allParticipantTime
        allParticipantTime.map { participantTime ->
            val participant =
                firestore.collection(PARTICIPANTS).document(participantTime.participantRef)
                    .get().data<Participant> { }
            participantTime.participant = participant
            participantTime
        }
        return allParticipantTime
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
        coroutineScope {
            recipe.shoppingIngredients.map { shoppingIngredient ->
                async {
                    shoppingIngredient.ingredient =
                        firestore.collection(INGREDIENT).document(shoppingIngredient.ingredientRef)
                            .get()
                            .data<Ingredient?> { }
                }
            }.awaitAll()
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

    override suspend fun createNewParticipant(participant: Participant) {
        Logger.i("Create New Participant")
        val participantId = generateRandomStringId()
        participant.uid = participantId
        participant.group = loginAndRegister.getCustomUserGroup()
        firestore.collection(PARTICIPANTS)
            .document(participant.uid)
            .set(participant)
    }

    override suspend fun updateParticipant(participant: Participant) {
        firestore.collection(PARTICIPANTS).document(participant.uid).set(participant)
    }

    override suspend fun deleteParticipant(participantId: String) {
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
            participantRef = newParticipant.uid
        )
        firestore.collection(EVENTS)
            .document(event.uid)
            .collection(PARTICIPANT_SCHEDULE)
            .document(participantId)
            .set(participant)
        return participant
    }
}