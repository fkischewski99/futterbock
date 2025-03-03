package view.event.recepie_overview_screen

import androidx.compose.foundation.Image
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
import model.Season
import model.ShoppingIngredient
import model.TimeRange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState

@Composable
fun RecipeOverviewScreen(navHostController: NavHostController) {
    val recipeOverviewViewModel: RecipeOverviewViewModel = koinInject()
    val state = recipeOverviewViewModel.recipeState.collectAsState()

    RecipeOverview(
        state = state.value,
        onAction = { action -> handleNavigation(navHostController, action) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeOverview(
    state: ResultState<RecipeOverviewState>,
    onAction: (NavigationActions) -> Unit
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
                ) {
                    RecipeDetails(recipeSelection = state.data.recipeSelection)
                    IngredientList(groupedIngredients = state.data.calculatedIngredientAmounts.groupBy { it.title })
                    MaterialList(materials = state.data.recipeSelection.recipe!!.materials)
                    CookingInstructions(instructions = state.data.recipeSelection.recipe!!.cookingInstructions)
                    Notes(notes = state.data.recipeSelection.recipe!!.notes)
                }
            }

            is ResultState.Error -> Text("Fehler beim Abrufen des Rezepts")
            ResultState.Loading -> MGCircularProgressIndicator()
        }
    }
}

@Composable
fun RecipeDetails(recipeSelection: RecipeSelection) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "${recipeSelection.selectedRecipeName} (${recipeSelection.eaterIds.size}" +
                        "${if (recipeSelection.eaterIds.size == 1) " Person" else " Personen"})",
                style = MaterialTheme.typography.titleLarge,
            )
            VerticalDivider(modifier = Modifier.padding(top = 12.dp))
            if (recipeSelection.recipe!!.description != "") {
                CaptionedText(label = "Beschreibung:", text = recipeSelection.recipe!!.description)
            }
            CaptionedText(label = "Preis:", text = "${recipeSelection.recipe!!.price}")
            if (recipeSelection.recipe!!.season.isNotEmpty()) {
                println(recipeSelection.recipe!!.season)
                CaptionedText(
                    label = "Saison:",
                    text = recipeSelection.recipe!!.season.joinToString { season: Season -> season.displayName })
            }
            if (recipeSelection.recipe!!.type.isNotEmpty()) {
                CaptionedText(
                    label = "Rezeptart:",
                    text = recipeSelection.recipe!!.type.joinToString { "," })
            }
            CaptionedText(
                label = "Futterbock Seite:",
                text = recipeSelection.recipe!!.pageInCookbook.toString()
            )
            CookingTime(timeRange = recipeSelection.recipe!!.time)
            SkillLevel(range = recipeSelection.recipe!!.skillLevel)
        }
    }
}

@Composable
fun DrawableIcon(res: DrawableResource, headline: String) {
    val imageModifier = Modifier
        .size(72.dp)

    Text(text = "$headline:", style = MaterialTheme.typography.titleSmall)
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Image(
            modifier = imageModifier,
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
fun IngredientList(groupedIngredients: Map<String?, List<ShoppingIngredient>>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            groupedIngredients.forEach { (ingredientsTitle, ingredientList) ->
                Text(
                    text = ingredientsTitle ?: "Zutaten:",
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                ingredientList.forEach { ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ingredient.getFormatedAmount(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        ingredient.ingredient?.let {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MaterialList(materials: List<String>) {
    val materialCounts = materials.groupingBy { it }.eachCount()
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Material:", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            materialCounts.forEach { (material, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$material ($count)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CookingInstructions(instructions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Kochanweisungen:", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            instructions.forEachIndexed { index, instruction ->
                Text(
                    text = "Schritt ${index + 1}:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun Notes(notes: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Notizen:", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            notes.forEach { note ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
