package data.sync

import co.touchlab.kermit.Logger
import data.FireBaseRepository
import data.local.AppDatabase
import kotlinx.coroutines.flow.first
import services.login.FirebaseLoginAndRegister

class InitialSyncService(
    private val db: AppDatabase,
    private val firebaseRepository: FireBaseRepository,
    private val loginAndRegister: FirebaseLoginAndRegister
) {
    suspend fun syncAllUserData() {
        if (!loginAndRegister.isAuthenticated()) return

        try {
            val group = loginAndRegister.getCustomUserGroup()
            Logger.i("InitialSync: Starting full sync for group $group")

            val recipes = firebaseRepository.getAllRecipes()
            recipes
                .filter { it.uid.isNotBlank() }
                .onEach { it.shoppingIngredients.forEach { si -> si.ingredient = null } }
                .forEach { db.recipeDao().insert(it) }
            Logger.i("InitialSync: Synced ${recipes.size} recipes")

            val ingredients = firebaseRepository.getAllIngredients()
            ingredients.filter { it.uid.isNotBlank() }.forEach { db.ingredientDao().insert(it) }
            Logger.i("InitialSync: Synced ${ingredients.size} ingredients")

            val materials = firebaseRepository.getAllMaterials()
            materials.filter { it.uid.isNotBlank() }.forEach { db.materialDao().insert(it) }
            Logger.i("InitialSync: Synced ${materials.size} materials")

            val events = firebaseRepository.getEventList(group).first()
            events.forEach { db.eventDao().insert(it) }
            Logger.i("InitialSync: Synced ${events.size} events")

            events.forEach { event ->
                try {
                    val participantTimes = firebaseRepository.getParticipantsOfEvent(event.uid, true)
                    participantTimes.forEach { pt ->
                        pt.eventId = event.uid
                        db.participantTimeDao().insert(pt)
                        if (pt.participant != null) {
                            db.participantDao().insert(pt.participant!!)
                        }
                    }

                    val meals = firebaseRepository.getAllMealsOfEvent(event.uid)
                    meals.forEach { meal ->
                        meal.eventId = event.uid
                        db.mealDao().insert(meal)
                    }

                    val shoppingList = firebaseRepository.getMultiDayShoppingList(event.uid)
                    if (shoppingList != null) {
                        db.multiDayShoppingListDao().insert(shoppingList)
                    }

                    val eventMaterials = firebaseRepository.getMaterialListOfEvent(event.uid)
                    eventMaterials.forEach { db.materialDao().insert(it) }
                } catch (e: Exception) {
                    Logger.e("InitialSync: Error syncing event ${event.uid}: ${e.message}")
                }
            }

            Logger.i("InitialSync: Full sync complete")
        } catch (e: Exception) {
            Logger.e("InitialSync: Sync failed: ${e.message}", e)
        }
    }
}
