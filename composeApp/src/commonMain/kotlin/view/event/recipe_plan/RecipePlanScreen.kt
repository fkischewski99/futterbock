package view.event.recipe_plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import getPlatformName
import kotlinx.datetime.LocalDate
import model.Meal
import org.koin.compose.koinInject
import view.event.EventState
import view.event.SharedEventViewModel
import view.event.actions.BaseAction
import view.event.actions.EditEventActions
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.login.ErrorField
import view.shared.HelperFunctions
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.page.ColumnWithPadding

@Composable
fun RecipePlanScreen(navController: NavHostController) {
    val sharedEventViewModel: SharedEventViewModel = koinInject()
    val sharedState = sharedEventViewModel.eventState.collectAsStateWithLifecycle()

    RecipePlanPage(
        state = sharedState.value,
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is EditEventActions -> sharedEventViewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePlanPage(
    state: ResultState<EventState>,
    onAction: (BaseAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Rezeptplan") },
                navigationIcon = {
                    NavigationIconButton(
                        onLeave = { onAction(NavigationActions.GoBack) }
                    )
                },
                actions = {
                    IconButton(
                        onClick = { onAction(EditEventActions.ShareRecipePlanPdf) },
                        modifier = Modifier.clip(RoundedCornerShape(75))
                            .background(MaterialTheme.colorScheme.tertiary)
                    ) {
                        val imageVector = when (getPlatformName()) {
                            "desktop" -> Icons.Default.Save
                            else -> Icons.Default.Share
                        }
                        Icon(
                            imageVector = imageVector,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            when (state) {
                is ResultState.Success -> {
                    RecipePlanContent(
                        eventName = state.data.event.name,
                        mealsGroupedByDate = state.data.mealsGroupedByDate
                    )
                }

                is ResultState.Loading -> {
                    ColumnWithPadding { MGCircularProgressIndicator() }
                }

                is ResultState.Error -> {
                    ColumnWithPadding { ErrorField(state.message) }
                }
            }
        }
    }
}

@Composable
fun RecipePlanContent(
    eventName: String,
    mealsGroupedByDate: Map<LocalDate, List<Meal>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = eventName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        mealsGroupedByDate.forEach { (date, meals) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = HelperFunctions.formatDate(date),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    meals.forEach { meal ->
                        MealItem(meal = meal)
                    }
                }
            }
        }
    }
}

@Composable
fun MealItem(meal: Meal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = meal.mealType.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (meal.recipeSelections.isNotEmpty()) {
            meal.recipeSelections.forEach { selection ->
                val personCount = selection.eaterIds.size + selection.guestCount
                Text(
                    text = "  • ${selection.selectedRecipeName} (${personCount} Personen)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            Text(
                text = "  Kein Rezept",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
