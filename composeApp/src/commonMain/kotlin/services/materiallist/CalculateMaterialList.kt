package services.materiallist

import data.EventRepository
import model.Material
import model.Source
import kotlin.math.max

class CalculateMaterialList(private val eventRepository: EventRepository) {

    suspend fun calculate(eventId: String): List<Material> {
        val materialSet = mutableSetOf<Material>()

        val meals = eventRepository.getMealsWithRecipeAndIngredients(eventId);
        meals.forEach { meal ->
            meal.recipeSelections.forEach { recipeSelection ->
                val materials = recipeSelection.recipe!!.materials
                val materialCounts = materials.groupingBy { it }.eachCount()
                materialCounts.forEach { (material, count) ->
                    val existingMaterial = materialSet.find { it.name == material }
                    if (existingMaterial != null) {
                        existingMaterial.amount = max(existingMaterial.amount, count)
                    } else {
                        materialSet.add(Material().apply {
                            name = material; amount = count; source = Source.COMPUTED
                        })
                    }
                }
            }
        }
        val mutableMaterials = materialSet.toMutableList()
        val existingMaterials = eventRepository.getMaterialListOfEvent(eventId)
        mutableMaterials.addAll(existingMaterials)
        return mutableMaterials.toList()
    }
}