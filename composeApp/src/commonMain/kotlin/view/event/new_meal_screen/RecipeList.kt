package view.event.new_meal_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import model.*

@Composable
fun RecipeList(
    allRecipes: List<Recipe>,
    searchText: String,
    filterForFoodIntolerance: Set<FoodIntolerance>,
    filterForEatingHabit: EatingHabit?,
    onRecipeSelected: (Recipe) -> Unit,
    filterForPrice: Range?,
    filterForTime: TimeRange?,
    filterForSkillLevel: Range?,
    filterForSeason: Season?,
    filterForRecipeType: RecipeType?
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        allRecipes
            .filter {
                it.matchesSearchQuery(
                    searchText,
                    filterForEatingHabit,
                    filterForFoodIntolerance,
                    filterForPrice,
                    filterForTime,
                    filterForRecipeType,
                    filterForSkillLevel,
                    filterForSeason
                )
            }
            .forEach { recipe ->
                RecipeItem(recipe = recipe, onClicked = onRecipeSelected)
            }
    }
}

@Composable
fun RecipeItem(recipe: Recipe, onClicked: (Recipe) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClicked(recipe) }
                .padding(16.dp)
        ) {
            Text("${recipe.name} - (Seite ${recipe.pageInCookbook})")
        }
        HorizontalDivider()
    }
}

@Composable
fun RecipeWithMembers(
    participants: List<ParticipantTime>,
    recipeSelection: RecipeSelection,
    onAction: (EditMealActions) -> Unit,
    canParticipantEatRecipe: (ParticipantTime, RecipeSelection) -> Boolean,
    getErrorMessage: suspend (ParticipantTime, RecipeSelection) -> String?
) {
    val checkedState = remember { mutableStateMapOf<String, Boolean>() }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(recipeSelection) {
        checkedState.clear()
        participants.forEach { participant ->
            checkedState[participant.participantRef] =
                recipeSelection.eaterIds.contains(participant.participantRef)
        }
    }

    val selectedParticipants = participants.filter { checkedState[it.participantRef] == true }
    var allSelectedCanEat by remember { mutableStateOf(true) }
    LaunchedEffect(selectedParticipants, recipeSelection) {
        allSelectedCanEat =
            selectedParticipants.all { canParticipantEatRecipe(it, recipeSelection) }
    }

    Column {
        SelectAllRow(
            expanded = expanded,
            onToggleExpand = { expanded = !expanded },
            isAllSelected = checkedState.values.all { it },
            canAllEat = allSelectedCanEat,
            onToggleAll = { isChecked ->
                participants.forEach { participant ->
                    checkedState[participant.participantRef] = isChecked
                    val action = if (isChecked) {
                        EditMealActions.AddEaterToRecipe(recipeSelection, participant)
                    } else {
                        EditMealActions.RemoveEaterFromRecipe(recipeSelection, participant)
                    }
                    onAction(action)
                }
            }
        )

        if (expanded) {
            participants.forEach { participant ->
                val isChecked = checkedState[participant.participantRef] ?: false
                val canEat = canParticipantEatRecipe(participant, recipeSelection)

                ParticipantCheckboxRow(
                    participant = participant,
                    recipeSelection = recipeSelection,
                    isChecked = isChecked,
                    canEat = canEat,
                    getErrorMessage = getErrorMessage,
                    onCheckedChange = { checked ->
                        checkedState[participant.participantRef] = checked
                        val action = if (checked) {
                            EditMealActions.AddEaterToRecipe(recipeSelection, participant)
                        } else {
                            EditMealActions.RemoveEaterFromRecipe(recipeSelection, participant)
                        }
                        onAction(action)
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectAllRow(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    isAllSelected: Boolean,
    canAllEat: Boolean,
    onToggleAll: (Boolean) -> Unit
) {
    val checkboxColor = getCheckboxColorForErrorState(!canAllEat)
    val textColor = getTextColorForErrorState(!canAllEat)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Checkbox(
            checked = isAllSelected,
            onCheckedChange = onToggleAll,
            colors = CheckboxDefaults.colors(
                checkedColor = checkboxColor,
                uncheckedColor = checkboxColor
            )
        )
        Text(
            text = if (isAllSelected) "Alle entfernen" else "Alle hinzufügen",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = textColor
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ParticipantCheckboxRow(
    participant: ParticipantTime,
    recipeSelection: RecipeSelection,
    isChecked: Boolean,
    canEat: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    getErrorMessage: suspend (ParticipantTime, RecipeSelection) -> String?
) {
    val hasError = !canEat && isChecked
    var showErrorDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = getCheckboxColorForErrorState(hasError)
            )
        )
        Text(
            text = participant.getListItemTitle(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).clickable { showErrorDialog = true },
            color = getTextColorForErrorState(hasError)
        )
        if (hasError) {
            IconButton(onClick = { showErrorDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Fehlerinformation",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showErrorDialog && !canEat) {
        val errorMessage = runBlocking { getErrorMessage(participant, recipeSelection) } ?: ""
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun getCheckboxColorForErrorState(hasError: Boolean) =
    if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

@Composable
private fun getTextColorForErrorState(hasError: Boolean) =
    if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
