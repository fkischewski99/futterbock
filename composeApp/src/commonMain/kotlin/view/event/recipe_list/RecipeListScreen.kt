package view.event.recipe_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import model.EatingHabit
import model.FoodIntolerance
import model.Range
import model.RecipeType
import model.Season
import model.TimeRange
import org.koin.compose.koinInject
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.categorized_shopping_list.IngredientViewModel
import view.event.new_meal_screen.*
import view.event.recepie_overview_screen.RecipeOverviewActions
import view.event.recepie_overview_screen.RecipeOverviewViewModel
import view.navigation.Routes
import view.shared.NavigationIconButton
import view.shared.ResultState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeListScreen(navController: NavHostController) {
    val recipeViewModel: RecipeViewModel = koinInject()
    val recipeOverviewViewModel: RecipeOverviewViewModel = koinInject()
    val ingredientViewModel: IngredientViewModel = koinInject()

    val allRecipes by recipeViewModel.state.collectAsStateWithLifecycle()
    val ingredientsState = ingredientViewModel.state.collectAsState()
    val allIngredients = when (val state = ingredientsState.value) {
        is ResultState.Success -> state.data
        else -> emptyList()
    }

    var searchText by remember { mutableStateOf("") }
    var selectedEatingHabitFilter by remember { mutableStateOf<EatingHabit?>(null) }
    var selectedFoodIntoleranceFilter by remember { mutableStateOf(emptySet<FoodIntolerance>()) }
    var selectedPriceFilter by remember { mutableStateOf<Range?>(null) }
    var selectedTimeFilter by remember { mutableStateOf<TimeRange?>(null) }
    var selectedRecipeTypeFilter by remember { mutableStateOf<RecipeType?>(null) }
    var selectedSeasonFilter by remember { mutableStateOf<Season?>(null) }
    var selectedSkillLevelFilter by remember { mutableStateOf<Range?>(null) }
    var selectedIngredientFilters by remember { mutableStateOf(setOf<String>()) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            RecipeListTopBar(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onNavigateBack = {
                    handleNavigation(navController, NavigationActions.GoBack)
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Filter chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                EatingHabitFilter(
                    selectedEatingHabitFilter = selectedEatingHabitFilter,
                    onFilterSelect = { selectedEatingHabitFilter = it }
                )
                FoodIntoleranceFilter(
                    selectedIntolerances = selectedFoodIntoleranceFilter,
                    onFiltersChange = { selectedFoodIntoleranceFilter = it }
                )
                PriceFilter(
                    onFilterSelect = { selectedPriceFilter = it },
                    selectedPriceFilter = selectedPriceFilter
                )
                TimeFilter(
                    onFilterSelect = { selectedTimeFilter = it },
                    selectedTimeFilter = selectedTimeFilter
                )
                RecipeTypeFilter(
                    onFilterSelect = { selectedRecipeTypeFilter = it },
                    selectedRecipeType = selectedRecipeTypeFilter
                )
                SeasonFilter(
                    onFilterSelect = { selectedSeasonFilter = it },
                    selectedRecipeType = selectedSeasonFilter
                )
                SkillLevelFilter(
                    onFilterSelect = { selectedSkillLevelFilter = it },
                    selectedRecipeType = selectedSkillLevelFilter
                )
                IngredientFilter(
                    selectedIngredientIds = selectedIngredientFilters,
                    onIngredientsChange = { selectedIngredientFilters = it },
                    allIngredients = allIngredients
                )
            }

            HorizontalDivider(thickness = 2.dp)

            // Recipe list
            RecipeList(
                allRecipes = allRecipes,
                searchText = searchText,
                filterForFoodIntolerance = selectedFoodIntoleranceFilter,
                filterForEatingHabit = selectedEatingHabitFilter,
                onRecipeSelected = { recipe ->
                    recipeOverviewViewModel.handleAction(
                        RecipeOverviewActions.InitializeScreenWithRecipeId(recipe.uid)
                    )
                    navController.navigate(Routes.RecipeOverview(recipe.uid))
                },
                filterForPrice = selectedPriceFilter,
                filterForTime = selectedTimeFilter,
                filterForSkillLevel = selectedSkillLevelFilter,
                filterForSeason = selectedSeasonFilter,
                filterForRecipeType = selectedRecipeTypeFilter,
                selectedIngredientFilters = selectedIngredientFilters
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeListTopBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column {
        TopAppBar(
            title = { Text("Rezeptliste") },
            navigationIcon = {
                NavigationIconButton(onLeave = onNavigateBack)
            }
        )
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Rezept suchen...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Suchen"
                )
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Löschen"
                        )
                    }
                }
            },
            singleLine = true
        )
        HorizontalDivider()
    }
}
