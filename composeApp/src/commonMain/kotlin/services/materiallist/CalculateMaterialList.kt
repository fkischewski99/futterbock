package services.materiallist

import data.EventRepository
import kotlin.math.max

class CalculateMaterialList(private val eventRepository: EventRepository) {

    suspend fun calculate(eventId: String): Map<String, Int> {
        val materialMap = mutableMapOf<String, Int>();

        val meals = eventRepository.getMealsWithRecipeAndIngredients(eventId);
        meals.forEach { meal ->
            meal.recipeSelections.forEach { recipeSelection ->
                val materials = recipeSelection.recipe!!.materials
                val materialCounts = materials.groupingBy { it }.eachCount()
                materialCounts.forEach { (material, count) ->
                    val currentCount = materialMap.getOrPut(
                        key = material,
                        defaultValue = { 0 }
                    )
                    materialMap[material] = max(currentCount, count)
                }
            }
        }
        return materialMap
    }
}