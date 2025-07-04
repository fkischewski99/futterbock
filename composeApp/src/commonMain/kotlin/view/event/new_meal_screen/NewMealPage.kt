package view.event.new_meal_screen

import ConfirmDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import model.EatingHabit
import model.FoodIntolerance
import model.ParticipantTime
import model.Range
import model.Recipe
import model.RecipeSelection
import model.RecipeType
import model.Season
import model.TimeRange
import org.koin.compose.koinInject
import services.event.ParticipantCanEatRecipe
import view.event.EventState
import view.event.SharedEventViewModel
import view.event.actions.BaseAction
import view.event.actions.EditEventActions
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.recepie_overview_screen.RecipeOverviewActions
import view.event.recepie_overview_screen.RecipeOverviewViewModel
import view.navigation.Routes
import view.shared.HelperFunctions
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.step_counter.StepperCounter

@Composable
fun EditMealScreen(
    navController: NavHostController,
) {
    val sharedEventViewModel: SharedEventViewModel = koinInject()
    val recipeOverviewViewModel: RecipeOverviewViewModel = koinInject()
    val state = sharedEventViewModel.eventState.collectAsStateWithLifecycle()
    val recipeViewModel: RecipeViewModel = koinInject()
    val allRecipesState = recipeViewModel.state.collectAsState()
    val canParticipantEat: ParticipantCanEatRecipe = koinInject()

    NewMealPage(
        state = state.value,
        allRecipes = allRecipesState.value,
        onAction = { action ->
            when (action) {
                is EditMealActions.ViewRecipe -> recipeOverviewViewModel.handleAction(
                    RecipeOverviewActions.InitializeScreen(
                        action.recipeSelection,
                        state.value.getSuccessData()?.event?.uid
                    )
                )

                is NavigationActions -> handleNavigation(navController, action)
                else -> sharedEventViewModel.onAction(action)
            }
        },
        participantCanEatRecipe = { participant: ParticipantTime, recipeSelection: RecipeSelection ->
            canParticipantEat.canParticipantEatRecipe(
                member = participant,
                recipeSelection = recipeSelection
            )
        },
        getRecipeEatingError = { participant: ParticipantTime, recipeSelection: RecipeSelection ->
            canParticipantEat.getErrorMessageForParticipant(
                member = participant,
                recipeSelection = recipeSelection
            )
        },
    )
}

@Composable
fun NewMealPage(
    state: ResultState<EventState>,
    allRecipes: List<Recipe>,
    onAction: (BaseAction) -> Unit,
    participantCanEatRecipe: (ParticipantTime, RecipeSelection) -> Boolean,
    getRecipeEatingError: suspend (ParticipantTime, RecipeSelection) -> String?,
) {
    var showDialog by remember { mutableStateOf(false) }
    var recipeToDelete: RecipeSelection? = null;
    var isSearchBarActive by remember { mutableStateOf(false) }

    //val context = LocalContext.current
    Scaffold(
        topBar = {
            SearchBarComponent(
                onAction = onAction,
                state = state,
                allRecipes = allRecipes,
                isSearchBarActive = isSearchBarActive,
                onSearchBarActiveChange = { isSearchBarActive = it }
            )
        },
        modifier = Modifier.fillMaxHeight(),
        floatingActionButton = {
            if (state is ResultState.Success) {
                ActionButtons(
                    onAction = onAction,
                    state = state.data,
                    onOpenSearch = { isSearchBarActive = true }
                )
            }
        }
    ) {

        Column(
            modifier = Modifier.padding(top = it.calculateTopPadding()).padding(8.dp)
                .verticalScroll(rememberScrollState()).fillMaxHeight()
        ) {
            when (state) {
                is ResultState.Success -> {
                    Spacer(modifier = Modifier.height(24.dp))
                    DateDropdown(
                        resultState = state.data,
                        onAction = onAction
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DropdownMenuMeal(state.data.selectedMeal.mealType, onAction = onAction)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Rezepte", style = MaterialTheme.typography.headlineSmall)
                    state.data.selectedMeal.recipeSelections.forEach {
                        Card(
                            modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Rezept ansehen",
                                    modifier = Modifier.padding(start = 16.dp).clickable {
                                        navigateToRecipe(onAction, it)
                                    })
                                Text(
                                    "${it.selectedRecipeName} (${it.eaterIds.size + it.guestCount} ${if (it.eaterIds.size + it.guestCount == 1) "Person" else "Personen"})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(8.dp).clickable {
                                        navigateToRecipe(onAction, it)
                                    },
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = {
                                        showDialog = true
                                        recipeToDelete = it
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Rezept löschen",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            RecipeWithMembers(
                                participants = state.data.currentParticipantsOfMeal,
                                recipeSelection = it,
                                onAction = onAction,
                                canParticipantEatRecipe = participantCanEatRecipe,
                                getErrorMessage = getRecipeEatingError
                            )
                        }
                        if (showDialog) {
                            ConfirmDialog(
                                {
                                    if (recipeToDelete != null)
                                        onAction(EditMealActions.DeleteRecipe(recipeToDelete!!))
                                },
                                { showDialog = false })
                        }
                    }
                }

                else -> {
                    MGCircularProgressIndicator()

                }
            }
        }
    }
}

private fun navigateToRecipe(
    onAction: (BaseAction) -> Unit,
    it: RecipeSelection
) {
    onAction(EditMealActions.ViewRecipe(it))
    onAction(
        NavigationActions.GoToRoute(
            Routes.RecipeOverview(
                it.recipeRef
            )
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchBarComponent(
    state: ResultState<EventState>,
    allRecipes: List<Recipe>,
    onAction: (BaseAction) -> Unit,
    isSearchBarActive: Boolean,
    onSearchBarActiveChange: (Boolean) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedEatingHabitFilter: EatingHabit? by remember { mutableStateOf(null) }
    var selectedFoodIntoleranceFilter by remember { mutableStateOf(emptySet<FoodIntolerance>()) }
    var selectedPriceFilter: Range? by remember { mutableStateOf(null) }
    var selectedTimeFilter: TimeRange? by remember { mutableStateOf(null) }
    var selectedRecipeTypeFilter: RecipeType? by remember { mutableStateOf(null) }
    var selectedSeasonFilter: Season? by remember { mutableStateOf(null) }
    var selectedSkillLevelFilter: Range? by remember { mutableStateOf(null) }


    when (state) {
        is ResultState.Success -> {
            val colors = SearchBarDefaults.colors()
            SearchBar(
                inputField = {
                    SearchInputField(
                        searchText = searchText,
                        onQueryChange = { searchText = it },
                        onSearch = { onSearchBarActiveChange(false) },
                        isActive = isSearchBarActive,
                        onActiveChange = onSearchBarActiveChange,
                        onNavigateBack = {
                            onAction(EditMealActions.SaveMeal)
                            onAction(NavigationActions.GoBack)

                        }
                    )
                },
                expanded = isSearchBarActive,
                onExpandedChange = onSearchBarActiveChange,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                colors = colors,
                content = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .padding(1.dp),
                    ) {
                        EatingHabitFilter(
                            selectedEatingHabitFilter = selectedEatingHabitFilter,
                            onFilterSelect = {
                                selectedEatingHabitFilter = it
                            },
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
                    }
                    HorizontalDivider(thickness = 4.dp)
                    RecipeList(
                        allRecipes = allRecipes,
                        searchText = searchText,
                        onRecipeSelected = { recipe ->
                            onSearchBarActiveChange(false)
                            searchText = ""
                            onAction(EditMealActions.SelectRecipe(recipe))
                        },
                        filterForEatingHabit = selectedEatingHabitFilter,
                        filterForFoodIntolerance = selectedFoodIntoleranceFilter,
                        filterForPrice = selectedPriceFilter,
                        filterForTime = selectedTimeFilter,
                        filterForRecipeType = selectedRecipeTypeFilter,
                        filterForSkillLevel = selectedSkillLevelFilter,
                        filterForSeason = selectedSeasonFilter
                    )
                }
            )
        }

        is ResultState.Error -> {
            Text("Fehler beim Abrufen der Rezepte")
        }

        ResultState.Loading -> {
            Text("Laden ...")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInputField(
    searchText: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {

    SearchBarDefaults.InputField(
        query = searchText,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = isActive,
        onExpandedChange = onActiveChange,
        placeholder = { Text("Rezept hinzufügen") },
        leadingIcon
        = {
            if (!isActive) {
                NavigationIconButton(
                    onLeave = onNavigateBack
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Suche"
                )
            }
        },
        trailingIcon = {
            if (isActive) {
                Icon(
                    modifier = Modifier.clickable {
                        if (searchText.isEmpty()) {
                            onActiveChange(false)
                        } else {
                            onQueryChange("")
                        }
                    },
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
    )
}


@Composable
fun ActionButtons(
    state: EventState,
    onAction: (BaseAction) -> Unit,
    onOpenSearch: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        ExtendedFloatingActionButton(
            onClick = onOpenSearch,
            modifier = Modifier
                .width(400.dp)
                .clip(shape = RoundedCornerShape(75)),
            containerColor = MaterialTheme.colorScheme.secondary,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Rezept hinzufügen"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rezept hinzufügen")
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                onAction(
                    EditEventActions.AddNewMeal(
                        HelperFunctions.getLocalDate(state.selectedMeal.day)
                    )
                )
            },
            modifier = Modifier
                .width(400.dp)
                .clip(shape = RoundedCornerShape(75)),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Icon"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Weitere Mahlzeit anlegen")
            }
        }
    }
}