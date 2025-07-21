package view.admin.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import model.*
import org.koin.compose.koinInject
import view.admin.new_participant.IngredientPickerDialog
import view.event.categorized_shopping_list.IngredientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormDialog(
    recipe: Recipe? = null, // null for create, non-null for edit
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val ingredientViewModel: IngredientViewModel = koinInject()
    val allIngredients by ingredientViewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    
    var showIngredientPicker by remember { mutableStateOf(false) }
    var editingIngredientIndex by remember { mutableStateOf(-1) }
    
    // Recipe form state
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var description by remember { mutableStateOf(recipe?.description ?: "") }
    var cookingInstructions by remember { mutableStateOf(recipe?.cookingInstructions ?: listOf()) }
    var notes by remember { mutableStateOf(recipe?.notes ?: listOf()) }
    var dietaryHabit by remember { mutableStateOf(recipe?.dietaryHabit ?: EatingHabit.OMNIVORE) }
    var shoppingIngredients by remember { mutableStateOf(recipe?.shoppingIngredients ?: listOf()) }
    var materials by remember { mutableStateOf(recipe?.materials ?: listOf()) }
    var price by remember { mutableStateOf(recipe?.price ?: Range.MEDIUM) }
    var season by remember { mutableStateOf(recipe?.season ?: listOf()) }
    var foodIntolerances by remember { mutableStateOf(recipe?.foodIntolerances ?: listOf()) }
    var time by remember { mutableStateOf(recipe?.time ?: TimeRange.MEDIUM) }
    var skillLevel by remember { mutableStateOf(recipe?.skillLevel ?: Range.MEDIUM) }
    var recipeType by remember { mutableStateOf(recipe?.type ?: listOf()) }
    
    // UI state for expanding sections
    var expandedSections by remember { mutableStateOf(setOf("basic")) }
    
    // Form validation
    val isValid = name.isNotBlank() && shoppingIngredients.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (recipe == null) "Neues Rezept" else "Rezept bearbeiten",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Abbrechen")
                        }
                        
                        Button(
                            onClick = {
                                val newRecipe = Recipe().apply {
                                    uid = recipe?.uid ?: ""
                                    this.name = name
                                    this.description = description
                                    this.cookingInstructions = cookingInstructions
                                    this.notes = notes
                                    this.dietaryHabit = dietaryHabit
                                    this.shoppingIngredients = shoppingIngredients
                                    this.materials = materials
                                    // Preserve existing values for fields user cannot edit
                                    this.pageInCookbook = recipe?.pageInCookbook ?: 0
                                    this.source = recipe?.source ?: "" // Will be set by repository
                                    this.price = price
                                    this.season = season
                                    this.foodIntolerances = foodIntolerances
                                    this.time = time
                                    this.skillLevel = skillLevel
                                    this.type = recipeType
                                }
                                onSave(newRecipe)
                            },
                            enabled = isValid
                        ) {
                            Text("Speichern")
                        }
                    }
                }
                
                Divider()
                
                // Form content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Basic Information Section
                    item {
                        ExpandableSection(
                            title = "Grundinformationen",
                            expanded = expandedSections.contains("basic"),
                            onExpandedChange = { expanded ->
                                expandedSections = if (expanded) {
                                    expandedSections + "basic"
                                } else {
                                    expandedSections - "basic"
                                }
                            }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Rezeptname *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = name.isBlank(),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Beschreibung") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    )
                                )
                            }
                        }
                    }
                    
                    // Ingredients Section
                    item {
                        ExpandableSection(
                            title = "Zutaten *",
                            expanded = expandedSections.contains("ingredients"),
                            onExpandedChange = { expanded ->
                                expandedSections = if (expanded) {
                                    expandedSections + "ingredients"
                                } else {
                                    expandedSections - "ingredients"
                                }
                            }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (shoppingIngredients.isEmpty()) "Keine Zutaten hinzugefügt" else "${shoppingIngredients.size} Zutaten",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Button(
                                        onClick = { 
                                            editingIngredientIndex = -1
                                            showIngredientPicker = true 
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Zutat hinzufügen")
                                    }
                                }
                                
                                shoppingIngredients.forEachIndexed { index, ingredient ->
                                    IngredientItem(
                                        ingredient = ingredient,
                                        onEdit = {
                                            editingIngredientIndex = index
                                            showIngredientPicker = true
                                        },
                                        onDelete = {
                                            shoppingIngredients = shoppingIngredients.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Properties Section
                    item {
                        ExpandableSection(
                            title = "Eigenschaften",
                            expanded = expandedSections.contains("properties"),
                            onExpandedChange = { expanded ->
                                expandedSections = if (expanded) {
                                    expandedSections + "properties"
                                } else {
                                    expandedSections - "properties"
                                }
                            }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Dietary Habit
                                DropdownSelector(
                                    label = "Ernährungsweise",
                                    options = EatingHabit.entries,
                                    selectedOption = dietaryHabit,
                                    onOptionSelected = { dietaryHabit = it },
                                    displayText = { it.toString() }
                                )
                                
                                // Time Range
                                DropdownSelector(
                                    label = "Zeitaufwand",
                                    options = TimeRange.entries,
                                    selectedOption = time,
                                    onOptionSelected = { time = it },
                                    displayText = { it.displayName }
                                )
                                
                                // Price Range
                                DropdownSelector(
                                    label = "Preis",
                                    options = Range.entries,
                                    selectedOption = price,
                                    onOptionSelected = { price = it },
                                    displayText = { it.displayName }
                                )
                                
                                // Skill Level
                                DropdownSelector(
                                    label = "Schwierigkeitsgrad",
                                    options = Range.entries,
                                    selectedOption = skillLevel,
                                    onOptionSelected = { skillLevel = it },
                                    displayText = { it.displayName }
                                )
                            }
                        }
                    }
                    
                    // Additional Information Section
                    item {
                        ExpandableSection(
                            title = "Zusätzliche Informationen",
                            expanded = expandedSections.contains("additional"),
                            onExpandedChange = { expanded ->
                                expandedSections = if (expanded) {
                                    expandedSections + "additional"
                                } else {
                                    expandedSections - "additional"
                                }
                            }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Cooking Instructions
                                StringListEditor(
                                    label = "Kochanleitungen",
                                    items = cookingInstructions,
                                    onItemsChanged = { cookingInstructions = it },
                                    placeholder = "Schritt hinzufügen...",
                                    showNumbering = true
                                )
                                
                                // Notes
                                StringListEditor(
                                    label = "Notizen",
                                    items = notes,
                                    onItemsChanged = { notes = it },
                                    placeholder = "Notiz hinzufügen..."
                                )
                                
                                // Materials
                                StringListEditor(
                                    label = "Benötigte Materialien",
                                    items = materials,
                                    onItemsChanged = { materials = it },
                                    placeholder = "Material hinzufügen..."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showIngredientPicker) {
        RecipeIngredientPickerDialog(
            allIngredients = allIngredients,
            existingIngredient = if (editingIngredientIndex >= 0) shoppingIngredients[editingIngredientIndex] else null,
            onDismiss = { showIngredientPicker = false },
            onSave = { newIngredient ->
                shoppingIngredients = if (editingIngredientIndex >= 0) {
                    shoppingIngredients.toMutableList().apply {
                        set(editingIngredientIndex, newIngredient)
                    }
                } else {
                    shoppingIngredients + newIngredient
                }
                showIngredientPicker = false
            }
        )
    }
}