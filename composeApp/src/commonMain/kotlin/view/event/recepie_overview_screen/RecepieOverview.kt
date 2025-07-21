package view.event.recepie_overview_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import futterbock_app.composeapp.generated.resources.Res
import futterbock_app.composeapp.generated.resources.schwierigkeit1
import futterbock_app.composeapp.generated.resources.schwierigkeit2
import futterbock_app.composeapp.generated.resources.schwierigkeit3
import futterbock_app.composeapp.generated.resources.zeitangabe1
import futterbock_app.composeapp.generated.resources.zeitangabe2
import futterbock_app.composeapp.generated.resources.zeitangabe3
import model.Range
import model.RecipeSelection
import model.RecipeType
import model.Season
import model.ShoppingIngredient
import model.TimeRange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.step_counter.StepperCounter

@Composable
fun RecipeOverviewScreen(navHostController: NavHostController) {
    val recipeOverviewViewModel: RecipeOverviewViewModel = koinInject()
    val state = recipeOverviewViewModel.recipeState.collectAsState()

    RecipeOverview(
        state = state.value,
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navHostController, action)
                is RecipeOverviewActions -> recipeOverviewViewModel.handleAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeOverview(
    state: ResultState<RecipeOverviewState>,
    onAction: (BaseAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Rezeptübersicht") },
                navigationIcon = {
                    NavigationIconButton(
                        onLeave = { onAction(NavigationActions.GoBack) }
                    )
                }
            )
        }
    ) { innerPadding ->
        when (state) {
            is ResultState.Success -> {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(bottom = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PortionCounter(recipeOverviewState = state.data, onAction = onAction)
                    RecipeDetails(recipeSelection = state.data.recipeSelection)
                    IngredientList(ingredientList = state.data.calculatedIngredientAmounts)
                    MaterialList(materials = state.data.recipeSelection.recipe!!.materials)
                    CookingInstructions(instructions = state.data.recipeSelection.recipe!!.cookingInstructions)
                    Notes(notes = state.data.recipeSelection.recipe!!.notes)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            is ResultState.Error -> Text("Fehler beim Abrufen des Rezepts")
            ResultState.Loading -> MGCircularProgressIndicator()
        }
    }
}

@Composable
fun PortionCounter(
    recipeOverviewState: RecipeOverviewState,
    onAction: (BaseAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = recipeOverviewState.recipeSelection.recipe!!.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(20.dp))
            StepperCounter(
                initialValue = recipeOverviewState.numberOfPortions,
                onValueChange = { onAction(RecipeOverviewActions.UpdateNumberOfPortions(it)) },
                bottomContent = {
                    Text(
                        "Portionen",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        }
    }
}


@Composable
fun RecipeDetails(recipeSelection: RecipeSelection) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = recipeSelection.selectedRecipeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${recipeSelection.eaterIds.size + recipeSelection.guestCount}" +
                        if (recipeSelection.eaterIds.size + recipeSelection.guestCount == 1) " Person" else " Personen",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            if (recipeSelection.recipe!!.description != "") {
                CaptionedText(label = "Beschreibung:", text = recipeSelection.recipe!!.description)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CookingTime(timeRange = recipeSelection.recipe!!.time)
                SkillLevel(range = recipeSelection.recipe!!.skillLevel)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Separate card for price and other details with icons
            Column(modifier = Modifier.padding(16.dp)) {
                CaptionedText(label = "Preis:", text = "${recipeSelection.recipe!!.price}")
                if (recipeSelection.recipe!!.season.isNotEmpty()) {
                    CaptionedText(
                        label = "Saison:",
                        text = recipeSelection.recipe!!.season.joinToString { season: Season -> season.displayName })
                }
                if (recipeSelection.recipe!!.season.isNotEmpty()) {
                    CaptionedText(
                        label = "Essgewohnheit:",
                        text = recipeSelection.recipe!!.dietaryHabit.toString()
                    )
                }
                if (recipeSelection.recipe!!.type.isNotEmpty()) {
                    CaptionedText(
                        label = "Rezeptart:",
                        text = recipeSelection.recipe!!.type.joinToString { type: RecipeType -> type.displayName })
                }
                val pageText = if (recipeSelection.recipe!!.pageInCookbook > 0) {
                    recipeSelection.recipe!!.pageInCookbook.toString()
                } else {
                    "selbst erstellt"
                }
                CaptionedText(
                    label = "Futterbock Seite:",
                    text = pageText
                )
            }
        }

    }
}

@Composable
fun DrawableIcon(res: DrawableResource, headline: String) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$headline:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Image(
            modifier = Modifier.size(80.dp),
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun CookingTime(timeRange: TimeRange) {
    val res = when (timeRange) {
        TimeRange.SHORT -> Res.drawable.zeitangabe1
        TimeRange.MEDIUM -> Res.drawable.zeitangabe2
        TimeRange.LONG -> Res.drawable.zeitangabe3
    }
    DrawableIcon(res = res, headline = "Koch Zeit")
}

@Composable
fun SkillLevel(range: Range) {
    val res = when (range) {
        Range.LOW -> Res.drawable.schwierigkeit1
        Range.MEDIUM -> Res.drawable.schwierigkeit2
        Range.HIGH -> Res.drawable.schwierigkeit3
    }
    DrawableIcon(res = res, headline = "Skill Level")
}

@Composable
fun CaptionedText(label: String, text: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun IngredientList(ingredientList: List<ShoppingIngredient>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Zutaten",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Sort ingredients so that those with no amount appear at the bottom
            val sortedIngredients = ingredientList.sortedBy { ingredient ->
                if (ingredient.amount <= 0.0) 1 else 0
            }
            
            sortedIngredients.forEach { ingredient ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ingredient.getFormatedAmount(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ingredient.ingredient?.let {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialList(materials: List<String>) {
    if (materials.isEmpty()) return

    val materialCounts = materials.groupingBy { it }.eachCount()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Benötigte Materialien",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            materialCounts.forEach { (material, count) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = material,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (count > 1) {
                            Text(
                                text = "${count}x",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CookingInstructions(instructions: List<String>) {
    if (instructions.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Kochanweisungen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            instructions.forEachIndexed { index, instruction ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Schritt ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Notes(notes: List<String>) {
    if (notes.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Notizen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            notes.forEach { note ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
