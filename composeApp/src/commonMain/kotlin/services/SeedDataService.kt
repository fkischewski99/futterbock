package services

import co.touchlab.kermit.Logger
import data.AppModePreferences
import data.FireBaseRepository
import data.local.AppDatabase
import services.login.FirebaseLoginAndRegister

class SeedDataService(
    private val db: AppDatabase,
    private val prefs: AppModePreferences
) {
    suspend fun downloadSeedDataIfNeeded(): Boolean {
        if (prefs.isSeedDataDownloaded()) return true

        return try {
            val firebaseRepo = FireBaseRepository(FirebaseLoginAndRegister())

            val recipes = firebaseRepo.getAllRecipes()
            Logger.i("SeedData: Downloaded ${recipes.size} recipes")
            recipes.forEach { recipe ->
                recipe.shoppingIngredients.forEach { it.ingredient = null }
                if (recipe.uid.isNotBlank()) {
                    db.recipeDao().insert(recipe)
                }
            }

            val ingredients = firebaseRepo.getAllIngredients()
            Logger.i("SeedData: Downloaded ${ingredients.size} ingredients")
            ingredients.forEach { ingredient ->
                if (ingredient.uid.isNotBlank()) {
                    db.ingredientDao().insert(ingredient)
                }
            }

            val materials = firebaseRepo.getAllMaterials()
            Logger.i("SeedData: Downloaded ${materials.size} materials")
            materials.forEach { material ->
                if (material.uid.isNotBlank()) {
                    db.materialDao().insert(material)
                }
            }

            prefs.setSeedDataDownloaded(true)
            Logger.i("SeedData: Download complete")
            true
        } catch (e: Exception) {
            Logger.e("SeedData: Download failed: ${e.message}", e)
            false
        }
    }
}
