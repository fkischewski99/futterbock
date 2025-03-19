package view.event.new_meal_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.EatingHabit
import model.FoodIntolerance
import model.ParticipantTime
import model.Range
import model.Recipe
import model.RecipeSelection
import model.RecipeType
import model.Season
import model.TimeRange

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
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        allRecipes
            .filter {
                it.matchesSearchQuery(
                    searchText = searchText,
                    filterForEatingHabit = filterForEatingHabit,
                    filterForFoodIntolerance = filterForFoodIntolerance,
                    filterForPrice = filterForPrice,
                    filterForTime = filterForTime,
                    filterForRecipeType = filterForRecipeType,
                    filterForSkillLevel = filterForSkillLevel,
                    filterForSeason = filterForSeason
                )
            }
            .forEach { recipe ->
                RecipeItem(recipe = recipe, onClicked = onRecipeSelected)
            }
    }
}

@Composable
fun RecipeItem(recipe: Recipe, onClicked: (Recipe) -> Unit) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .clickable { onClicked(recipe) }
    ) {
        Text(text = recipe.name + " - (Seite " + recipe.pageInCookbook + ")")
    }
    HorizontalDivider()
}

@Composable
fun RecipeWithMembers(
    participants: List<ParticipantTime>,
    recipeSelection: RecipeSelection,
    onAction: (EditMealActions) -> Unit
) {

    val checkedState = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(recipeSelection) {
        checkedState.clear() // Clear existing states
        participants.forEach { participant ->
            checkedState[participant.getListItemTitle()] =
                recipeSelection.eaterIds.contains(participant.participant!!.uid)
        }
    }


    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checkedState.values.all { it }, onCheckedChange = { isChecked ->
                participants.forEach { member ->
                    checkedState[member.getListItemTitle()] = isChecked
                    if (isChecked) {
                        onAction(EditMealActions.AddEaterToRecipe(recipeSelection, member))
                    } else {
                        onAction(EditMealActions.RemoveEaterFromRecipe(recipeSelection, member))
                    }
                }
            })
            Text(
                text = "Alle auswählen",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        if (expanded) {
            participants.forEach { member ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checkedState[member.getListItemTitle()] ?: false,
                        onCheckedChange = { isChecked ->
                            checkedState[member.getListItemTitle()] = isChecked
                            if (isChecked) {
                                onAction(EditMealActions.AddEaterToRecipe(recipeSelection, member))
                            } else {
                                onAction(
                                    EditMealActions.RemoveEaterFromRecipe(
                                        recipeSelection,
                                        member
                                    )
                                )
                            }
                        })
                    Text(
                        text = member.getListItemTitle(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}