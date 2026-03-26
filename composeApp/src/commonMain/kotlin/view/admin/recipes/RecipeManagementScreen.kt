package view.admin.recipes

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.koin.compose.koinInject
import view.navigation.Routes
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeManagementScreen(
    navController: NavHostController
) {
    val viewModel: RecipeManagementViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("Rezepte verwalten") },
                navigationIcon = {
                    NavigationIconButton(
                        onLeave = { navController.popBackStack() }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.setRecipeForEdit(null)
                    navController.navigate(Routes.RecipeForm)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Neues Rezept erstellen"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ResultState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        MGCircularProgressIndicator()
                    }
                }

                is ResultState.Success -> {
                    RecipeList(
                        recipes = state.data,
                        onEditRecipe = { recipe ->
                            viewModel.setRecipeForEdit(recipe)
                            navController.navigate(Routes.RecipeForm)
                        },
                        onDeleteRecipe = { recipe ->
                            viewModel.onAction(RecipeManagementAction.DeleteRecipe(recipe))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }

                is ResultState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Fehler beim Laden der Rezepte: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
