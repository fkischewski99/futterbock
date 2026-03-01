package view.event.new_meal_screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import model.Ingredient
import view.admin.new_participant.IngredientPickerDialog

@Composable
fun IngredientFilter(
    selectedIngredientIds: Set<String>,
    onIngredientsChange: (Set<String>) -> Unit,
    allIngredients: List<Ingredient>
) {
    var showIngredientPicker by remember { mutableStateOf(false) }
    var isFilterActive by remember(selectedIngredientIds) {
        mutableStateOf(selectedIngredientIds.isNotEmpty())
    }

    // Create a display text for selected ingredients
    val displayText = if (isFilterActive) {
        val ingredientNames = selectedIngredientIds.mapNotNull { id ->
            allIngredients.find { it.uid == id }?.name
        }
        "Zutat: ${ingredientNames.joinToString(", ")}"
    } else {
        "Zutaten"
    }

    FilterChip(
        text = displayText,
        isSelected = isFilterActive,
        onClick = { showIngredientPicker = true }
    )

    // Ingredient picker dialog (multi-select mode)
    if (showIngredientPicker) {
        IngredientPickerDialog(
            onDismiss = { showIngredientPicker = false },
            onSelected = { ingredient ->
                // Toggle selection (add if not selected, remove if already selected)
                if (selectedIngredientIds.contains(ingredient.uid)) {
                    onIngredientsChange(selectedIngredientIds - ingredient.uid)
                } else {
                    onIngredientsChange(selectedIngredientIds + ingredient.uid)
                }
            },
            selectedIngredients = selectedIngredientIds.toList(),
            ingredientList = allIngredients,
            multiSelect = true
        )
    }
}
