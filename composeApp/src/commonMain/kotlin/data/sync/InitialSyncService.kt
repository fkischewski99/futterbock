package data.sync

import co.touchlab.kermit.Logger
import data.AppModePreferences
import data.FireBaseRepository
import data.local.AppDatabase
import kotlinx.coroutines.flow.first
import services.login.FirebaseLoginAndRegister

class InitialSyncService(
    private val db: AppDatabase,
    private val firebaseRepository: FireBaseRepository,
    private val loginAndRegister: FirebaseLoginAndRegister,
    private val networkMonitor: NetworkMonitor,
    private val prefs: AppModePreferences
) {
    suspend fun syncBaseData(): Boolean {
        if (!networkMonitor.isOnline.value) return false
        if (prefs.isSeedDataDownloaded()) return true

        return try {
            Logger.i("InitialSync: Syncing base data (recipes, ingredients, materials)")

            val recipes = firebaseRepository.getAllRecipes()
                .filter { it.uid.isNotBlank() }
                .onEach { it.shoppingIngredients.forEach { si -> si.ingredient = null } }
            db.recipeDao().insertAll(recipes)
            Logger.i("InitialSync: Synced ${recipes.size} recipes")

            val ingredients = firebaseRepository.getAllIngredients()
            db.ingredientDao().insertAll(ingredients.filter { it.uid.isNotBlank() })
            Logger.i("InitialSync: Synced ${ingredients.size} ingredients")

            val materials = firebaseRepository.getAllMaterials()
            db.materialDao().insertAll(materials.filter { it.uid.isNotBlank() })
            Logger.i("InitialSync: Synced ${materials.size} materials")

            prefs.setSeedDataDownloaded(true)
            Logger.i("InitialSync: Base data sync complete")
            true
        } catch (e: Exception) {
            Logger.e("InitialSync: Base data sync failed: ${e.message}", e)
            false
        }
    }

    suspend fun syncAllUserData() {
        if (!networkMonitor.isOnline.value) return
        if (!loginAndRegister.isAuthenticated()) return

        try {
            val group = loginAndRegister.getCustomUserGroup()
            Logger.i("InitialSync: Starting full sync for group $group")

            syncBaseData()

            val events = firebaseRepository.getEventList(group).first()
            db.eventDao().insertAll(events)
            Logger.i("InitialSync: Synced ${events.size} events")

            events.forEach { event ->
                try {
                    val participantTimes = firebaseRepository.getParticipantsOfEvent(event.uid, true)
                    participantTimes.forEach { pt ->
                        pt.eventId = event.uid
                    }
                    db.participantTimeDao().insertAll(participantTimes)
                    val participants = participantTimes.mapNotNull { it.participant }
                    if (participants.isNotEmpty()) {
                        db.participantDao().insertAll(participants)
                    }

                    val meals = firebaseRepository.getAllMealsOfEvent(event.uid)
                    meals.forEach { it.eventId = event.uid }
                    db.mealDao().insertAll(meals)

                    val shoppingList = firebaseRepository.getMultiDayShoppingList(event.uid)
                    if (shoppingList != null) {
                        db.multiDayShoppingListDao().insert(shoppingList)
                    }

                    val eventMaterials = firebaseRepository.getMaterialListOfEvent(event.uid)
                    eventMaterials.forEach { it.eventId = event.uid }
                    if (eventMaterials.isNotEmpty()) {
                        db.materialDao().insertAll(eventMaterials)
                    }
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
