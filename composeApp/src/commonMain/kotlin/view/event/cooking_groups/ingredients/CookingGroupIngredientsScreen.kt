package view.event.cooking_groups.ingredients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import model.ShoppingIngredient
import org.koin.compose.koinInject
import services.cookingGroups.CookingGroupIngredientService
import services.cookingGroups.CookingGroupIngredientService.CookingGroupIngredients
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.cooking_groups.CookingGroupActions
import view.event.cooking_groups.CookingGroupsContent
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState


@Composable
fun CookingGroupIngredientScreen(
    navController: NavHostController
) {
    val cookingGroupIngredientsViewModel: CookingGroupIngredientsViewModel = koinInject()
    val state = cookingGroupIngredientsViewModel.state.collectAsStateWithLifecycle()


    CookingGroupIngredientsPage(
        state = state.value,
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is CookingGroupIngredientActions -> cookingGroupIngredientsViewModel.onAction(action)
            }
        }
    )
}

/**
 * Screen displaying ingredient distribution per cooking group for a specific day.
 * Features day navigation and detailed ingredient lists for each cooking group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingGroupIngredientsPage(
    state: ResultState<CookingGroupIngredientsState>,
    onAction: (BaseAction) -> Unit,

    ) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zutatenverteilung Kochgruppen") },
                navigationIcon = {
                    NavigationIconButton(onLeave = { onAction(NavigationActions.GoBack) })
                },
                actions = {
                    IconButton(onClick = { onAction(CookingGroupIngredientActions.Refresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aktualisieren"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when (state) {
            is ResultState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    MGCircularProgressIndicator()
                }
            }

            is ResultState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Fehler beim Laden der Daten",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { onAction(CookingGroupIngredientActions.Refresh) }) {
                            Text("Erneut versuchen")
                        }
                    }
                }
            }

            is ResultState.Success -> {
                CookingGroupIngredientsContent(
                    state = state.data,
                    onPreviousDay = { onAction(CookingGroupIngredientActions.PreviousDay) },
                    onNextDay = { onAction(CookingGroupIngredientActions.NextDay) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun CookingGroupIngredientsContent(
    state: CookingGroupIngredientsState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Day Navigation
        DayNavigationCard(
            currentDate = state.currentDate,
            hasPreviousDay = state.hasPreviousDay,
            hasNextDay = state.hasNextDay,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay
        )


        // Loading indicator for data refresh
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        // Error message
        state.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Cooking Groups Content
        if (state.cookingGroupIngredients.isEmpty() && !state.isLoading) {
            EmptyStateCard()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.cookingGroupIngredients) { cookingGroupData ->
                    CookingGroupCard(cookingGroupData = cookingGroupData)
                }
                
                // Total ingredients summary at the bottom
                item {
                    TotalIngredientsSummaryCard(
                        cookingGroupIngredients = state.cookingGroupIngredients,
                        currentDate = state.currentDate
                    )
                }
            }
        }
    }
}

@Composable
private fun DayNavigationCard(
    currentDate: LocalDate,
    hasPreviousDay: Boolean,
    hasNextDay: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                enabled = hasPreviousDay
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Vorheriger Tag"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDate(currentDate),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatWeekday(currentDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextDay,
                enabled = hasNextDay
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Nächster Tag"
                )
            }
        }
    }
}

@Composable
private fun CookingGroupCard(
    cookingGroupData: CookingGroupIngredients
) {
    IngredientsCard(
        icon = Icons.Default.Group,
        title = cookingGroupData.cookingGroupName,
        subtitle = buildString {
            if (cookingGroupData.participantCount > 0) {
                append("${cookingGroupData.participantCount} Teilnehmer")
            }
            if (cookingGroupData.guestCount > 0) {
                if (cookingGroupData.participantCount > 0) append(" • ")
                append("${cookingGroupData.guestCount} Gäste")
            }
        },
        ingredients = cookingGroupData.ingredients
    )
}

@Composable
private fun IngredientsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    ingredients: List<ShoppingIngredient>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ingredients List
            if (ingredients.isEmpty()) {
                Text(
                    text = "Keine Zutaten für diesen Tag",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ingredients.groupBy { it.ingredient?.category ?: "Andere" }
                        .forEach { ingredientsByGroup ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ingredientsByGroup.key,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(thickness = 3.dp)
                            ingredientsByGroup.value.sortedBy { it.ingredient?.name }
                                .forEach { ingredient ->
                                    IngredientRow(ingredient = ingredient)
                                    HorizontalDivider(thickness = 1.dp)
                                }
                        }
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: ShoppingIngredient
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ingredient.ingredient?.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = ingredient.getFormatedAmount(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Keine Kochgruppen für diesen Tag",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Es wurden keine Mahlzeiten oder Teilnehmer für diesen Tag gefunden.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Formats a date for display (e.g., "15. März 2024")
 */
private fun formatDate(date: LocalDate): String {
    val months = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )
    return "${date.dayOfMonth}. ${months[date.monthNumber - 1]} ${date.year}"
}

/**
 * Formats the weekday for display (e.g., "Montag")
 */
private fun formatWeekday(date: LocalDate): String {
    val weekdays = listOf(
        "Montag", "Dienstag", "Mittwoch", "Donnerstag",
        "Freitag", "Samstag", "Sonntag"
    )
    // LocalDate.dayOfWeek returns 1-7 (Monday=1, Sunday=7)
    return weekdays[date.dayOfWeek.ordinal]
}

@Composable
private fun TotalIngredientsSummaryCard(
    cookingGroupIngredients: List<CookingGroupIngredients>,
    currentDate: LocalDate
) {
    // Calculate total ingredients across all cooking groups
    val totalIngredients = mutableMapOf<String, ShoppingIngredient>()
    
    cookingGroupIngredients.forEach { cookingGroup ->
        cookingGroup.ingredients.forEach { ingredient ->
            val key = ingredient.ingredientRef + ingredient.unit
            val existing = totalIngredients[key]
            
            if (existing != null) {
                existing.amount += ingredient.amount
            } else {
                totalIngredients[key] = ShoppingIngredient().apply {
                    this.ingredient = ingredient.ingredient
                    this.ingredientRef = ingredient.ingredientRef
                    this.unit = ingredient.unit
                    this.amount = ingredient.amount
                    this.nameEnteredByUser = ingredient.nameEnteredByUser
                    this.note = ingredient.note
                }
            }
        }
    }
    
    val totalCount = cookingGroupIngredients.sumOf { it.participantCount + it.guestCount }
    val subtitle = "Gesamtübersicht für $totalCount Personen"
    
    IngredientsCard(
        icon = Icons.Default.ShoppingCart,
        title = "Gesamte Zutaten",
        subtitle = subtitle,
        ingredients = totalIngredients.values.toList()
    )
}


