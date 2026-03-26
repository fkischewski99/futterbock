package view.admin.recipes

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import model.*
import org.koin.compose.koinInject
import view.event.categorized_shopping_list.IngredientViewModel
import view.shared.NavigationIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormScreen(
    navController: NavHostController
) {
    val viewModel: RecipeManagementViewModel = koinInject()
    val recipe = remember { viewModel.consumeRecipeForEdit() }

    RecipeFormPage(
        recipe = recipe,
        onSave = { newRecipe ->
            if (recipe != null) {
                viewModel.onAction(RecipeManagementAction.UpdateRecipe(newRecipe))
            } else {
                viewModel.onAction(RecipeManagementAction.CreateRecipe(newRecipe))
            }
            navController.popBackStack()
        },
        onBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormPage(
    recipe: Recipe? = null,
    onSave: (Recipe) -> Unit,
    onBack: () -> Unit
) {
    val ingredientViewModel: IngredientViewModel = koinInject()
    val allIngredients by ingredientViewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    var showIngredientPicker by remember { mutableStateOf(false) }
    var editingIngredientIndex by remember { mutableStateOf(-1) }
    val descriptionFocusRequester = remember { FocusRequester() }

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
    var foodIntolerances by remember {
        mutableStateOf(
            (recipe?.foodIntolerances ?: setOf(
                FoodIntolerance.FRUCTOSE_INTOLERANCE,
                FoodIntolerance.LACTOSE_INTOLERANCE,
                FoodIntolerance.GLUTEN_INTOLERANCE,
                FoodIntolerance.WITHOUT_NUTS
            )).toMutableSet()
        )
    }
    var time by remember { mutableStateOf(recipe?.time ?: TimeRange.MEDIUM) }
    var skillLevel by remember { mutableStateOf(recipe?.skillLevel ?: Range.MEDIUM) }
    var recipeType by remember { mutableStateOf(recipe?.type ?: listOf()) }

    // UI state for expanding sections
    var expandedSections by remember { mutableStateOf(setOf("basic")) }

    // Form validation
    val isValid = name.isNotBlank() && shoppingIngredients.isNotEmpty()

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text(if (recipe == null) "Neues Rezept" else "Rezept bearbeiten") },
                navigationIcon = {
                    NavigationIconButton(onLeave = onBack)
                },
                actions = {
                    IconButton(
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
                                this.pageInCookbook = recipe?.pageInCookbook ?: 0
                                this.source = recipe?.source ?: ""
                                this.price = price
                                this.season = season
                                this.foodIntolerances = foodIntolerances.toList()
                                this.time = time
                                this.skillLevel = skillLevel
                                this.type = recipeType
                            }
                            onSave(newRecipe)
                        },
                        enabled = isValid
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Speichern")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                onNext = { descriptionFocusRequester.requestFocus() }
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Beschreibung") },
                            modifier = Modifier.fillMaxWidth()
                                .focusRequester(descriptionFocusRequester),
                            minLines = 2,
                            maxLines = 4
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
                        if (shoppingIngredients.isNotEmpty()) {
                            Text(
                                text = "${shoppingIngredients.size} Zutaten",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = {
                                editingIngredientIndex = -1
                                showIngredientPicker = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Zutat hinzufügen")
                        }

                        shoppingIngredients.forEachIndexed { index, ingredient ->
                            IngredientItem(
                                ingredient = ingredient,
                                onEdit = {
                                    editingIngredientIndex = index
                                    showIngredientPicker = true
                                },
                                onDelete = {
                                    shoppingIngredients =
                                        shoppingIngredients.toMutableList().apply {
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
                foodIntolerances.removeAll(newIngredient.ingredient?.intolerances ?: listOf())
            }
        )
    }
}
