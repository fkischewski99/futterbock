package view.admin.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import model.*
import view.admin.new_participant.IngredientPickerDialog
import view.shared.ResultState
import view.shared.HelperFunctions.Companion.generateRandomStringId

@Composable
fun RecipeIngredientPickerDialog(
    allIngredients: ResultState<List<Ingredient>>,
    existingIngredient: ShoppingIngredient? = null,
    onDismiss: () -> Unit,
    onSave: (ShoppingIngredient) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIngredient by remember { 
        mutableStateOf<Ingredient?>(
            existingIngredient?.let { existing ->
                when (allIngredients) {
                    is ResultState.Success -> allIngredients.data.find { it.uid == existing.ingredientRef }
                    else -> null
                }
            }
        )
    }
    var amount by remember { mutableStateOf(existingIngredient?.amount?.toString() ?: "") }
    var selectedUnit by remember { mutableStateOf(existingIngredient?.unit ?: IngredientUnit.GRAMM) }
    var showIngredientPicker by remember { mutableStateOf(selectedIngredient == null) }

    val isValid = selectedIngredient != null && 
                  amount.isNotBlank() && 
                  amount.toDoubleOrNull() != null && 
                  amount.toDoubleOrNull()!! > 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (existingIngredient == null) "Zutat hinzufügen" else "Zutat bearbeiten",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Selected ingredient display or picker button
                if (selectedIngredient != null) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedIngredient!!.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            TextButton(onClick = { showIngredientPicker = true }) {
                                Text("Ändern")
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { showIngredientPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zutat auswählen")
                    }
                }

                // Amount input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Menge") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = amount.isNotBlank() && (amount.toDoubleOrNull() == null || amount.toDoubleOrNull()!! <= 0),
                    supportingText = {
                        if (amount.isNotBlank() && (amount.toDoubleOrNull() == null || amount.toDoubleOrNull()!! <= 0)) {
                            Text(
                                "Bitte geben Sie eine gültige Menge größer 0 ein",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // Unit selector
                DropdownSelector(
                    label = "Einheit",
                    options = IngredientUnit.entries,
                    selectedOption = selectedUnit,
                    onOptionSelected = { selectedUnit = it },
                    displayText = { it.toString() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = {
                            selectedIngredient?.let { ingredient ->
                                val shoppingIngredient = ShoppingIngredient().apply {
                                    nameEnteredByUser = ingredient.name
                                    ingredientRef = ingredient.uid
                                    uid = generateShoppingIngredientId()
                                    this.amount = amount.toDouble()
                                    unit = selectedUnit
                                    source = Source.ENTERED_BY_USER
                                    this.ingredient = ingredient
                                }
                                onSave(shoppingIngredient)
                            }
                        },
                        enabled = isValid
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }

    // Show ingredient picker when needed
    if (showIngredientPicker) {
        when (allIngredients) {
            is ResultState.Success -> {
                IngredientPickerDialog(
                    ingredientList = allIngredients.data,
                    onSelected = { ingredient ->
                        selectedIngredient = ingredient
                        showIngredientPicker = false
                    },
                    onDismiss = { showIngredientPicker = false },
                    selectedIngredients = selectedIngredient?.let { listOf(it.uid) } ?: emptyList()
                )
            }
            is ResultState.Loading -> {
                Dialog(onDismissRequest = { showIngredientPicker = false }) {
                    Card {
                        Box(
                            modifier = Modifier.padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            is ResultState.Error -> {
                AlertDialog(
                    onDismissRequest = { showIngredientPicker = false },
                    title = { Text("Fehler") },
                    text = { Text("Fehler beim Laden der Zutaten: ${allIngredients.message}") },
                    confirmButton = {
                        TextButton(onClick = { showIngredientPicker = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

private fun generateShoppingIngredientId(): String {
    return generateRandomStringId()
}