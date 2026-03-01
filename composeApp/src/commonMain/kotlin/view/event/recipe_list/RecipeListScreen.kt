package view.event.recipe_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.koin.compose.koinInject
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.new_meal_screen.RecipeList
import view.event.new_meal_screen.RecipeViewModel
import view.event.recepie_overview_screen.RecipeOverviewActions
import view.event.recepie_overview_screen.RecipeOverviewViewModel
import view.navigation.Routes
import view.shared.NavigationIconButton

@Composable
fun RecipeListScreen(navController: NavHostController) {
    val recipeViewModel: RecipeViewModel = koinInject()
    val recipeOverviewViewModel: RecipeOverviewViewModel = koinInject()
    val allRecipes by recipeViewModel.state.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
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
        Box(modifier = Modifier.padding(paddingValues)) {
            RecipeList(
                allRecipes = allRecipes,
                searchText = searchText,
                filterForFoodIntolerance = emptySet(),
                filterForEatingHabit = null,
                onRecipeSelected = { recipe ->
                    recipeOverviewViewModel.handleAction(
                        RecipeOverviewActions.InitializeScreenWithRecipeId(recipe.uid)
                    )
                    navController.navigate(Routes.RecipeOverview(recipe.uid))
                },
                filterForPrice = null,
                filterForTime = null,
                filterForSkillLevel = null,
                filterForSeason = null,
                filterForRecipeType = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
