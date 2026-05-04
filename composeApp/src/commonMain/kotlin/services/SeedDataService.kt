package services

import co.touchlab.kermit.Logger
import data.AppModePreferences
import data.FireBaseRepository
import data.local.AppDatabase

class SeedDataService(
    private val db: AppDatabase,
    private val prefs: AppModePreferences,
    private val firebaseRepo: FireBaseRepository
) {
    suspend fun downloadSeedDataIfNeeded(): Boolean {
        if (prefs.isSeedDataDownloaded()) return true

        return try {

            val recipes = firebaseRepo.getAllRecipes()
            Logger.i("SeedData: Downloaded ${recipes.size} recipes")
            recipes
                .filter { it.uid.isNotBlank() }
                .onEach { it.shoppingIngredients.forEach { si -> si.ingredient = null } }
                .forEach { db.recipeDao().insert(it) }

            val ingredients = firebaseRepo.getAllIngredients()
            Logger.i("SeedData: Downloaded ${ingredients.size} ingredients")
            ingredients
                .filter { it.uid.isNotBlank() }
                .forEach { db.ingredientDao().insert(it) }

            val materials = firebaseRepo.getAllMaterials()
            Logger.i("SeedData: Downloaded ${materials.size} materials")
            materials
                .filter { it.uid.isNotBlank() }
                .forEach { db.materialDao().insert(it) }

            prefs.setSeedDataDownloaded(true)
            Logger.i("SeedData: Download complete")
            true
        } catch (e: Exception) {
            Logger.e("SeedData: Download failed: ${e.message}", e)
            false
        }
    }
}
