package view.event.categorized_shopping_list

import CategorizedShoppingListViewModel
import ConfirmDialog
import ShoppingListState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.datetime.LocalDate
import model.Ingredient
import model.MultiDayShoppingList
import model.ShoppingIngredient
import view.shared.HelperFunctions
import org.koin.compose.koinInject
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.categorized_shopping_list.BottomSheetWithSearchBar
import services.shoppingList.shoppingDone
import view.login.ErrorField
import view.shared.EditTextDialog
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.page.ColumnWithPadding

@Composable
fun ShoppingListScreen(navController: NavHostController) {
    val viewModelShoppingList: CategorizedShoppingListViewModel = koinInject()
    val viewModelIngredient: IngredientViewModel = koinInject()
    val state = viewModelShoppingList.state.collectAsStateWithLifecycle()
    val ingredientState = viewModelIngredient.state.collectAsStateWithLifecycle()
    
    val ingredientList = when (val ingredientsState = ingredientState.value) {
        is ResultState.Success -> ingredientsState.data
        else -> emptyList()
    }

    ShoppingListCategorized(
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is EditShoppingListActions -> viewModelShoppingList.onAction(action)
            }
        },
        state = state.value,
        ingredientList = ingredientList
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListCategorized(
    onAction: (BaseAction) -> Unit,
    state: ResultState<ShoppingListState>,
    ingredientList: List<Ingredient>
) {
    when (state) {
        is ResultState.Success ->
            BottomSheetWithSearchBar(
                items = ingredientList,
                onItemAdded = { text -> onAction(EditShoppingListActions.AddNewIngredient(text)) },
                topBar = {
                    TopAppBar(title = {
                        Text(text = "Einkaufsliste")
                    }, navigationIcon = {
                        NavigationIconButton(
                            onLeave = {
                                onAction(EditShoppingListActions.SaveToEvent)
                                onAction(NavigationActions.GoBack)
                            }
                        )
                    })
                },
                content = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                            .padding(
                                start = 8.dp,
                                bottom = 24.dp,
                                end = 8.dp
                            )
                    ) {
                        // Multi-day navigation - always shown now
                        if (state.data.multiDayShoppingList != null) {
                            MultiDayNavigationBar(
                                multiDayShoppingList = state.data.multiDayShoppingList,
                                selectedDate = state.data.selectedDate,
                                currentState = state.data,
                                onDateSelected = { date ->
                                    onAction(EditShoppingListActions.SelectShoppingDay(date))
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        state.data.ingredientsByCategory.forEach { (category, ingredients) ->
                            ShoppingList(
                                ingredients, category,
                                onCheckboxClicked = {
                                    onAction(EditShoppingListActions.ToggleShoppingDone(it))
                                },
                                onDeleteShoppingItem = {
                                    onAction(
                                        EditShoppingListActions.DeleteShoppingItem(
                                            it
                                        )
                                    )
                                })
                        }
                    }
                },
            )

        is ResultState.Loading ->
            ColumnWithPadding { MGCircularProgressIndicator() }

        is ResultState.Error -> {
            ColumnWithPadding { ErrorField(state.message) }
        }
    }
}


@Composable
fun ShoppingList(
    ingredientsList: List<ShoppingIngredient>,
    category: String,
    onCheckboxClicked: (ShoppingIngredient) -> Unit = {},
    onDeleteShoppingItem: (ShoppingIngredient) -> Unit = {},
) {
    var categoryExpanded by remember { mutableStateOf(true) }
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { categoryExpanded = !categoryExpanded }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = category + " (" + ingredientsList.size + ")",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (categoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
            contentDescription = if (categoryExpanded) "Kategorie einklappen" else "Kategorie ausklappen",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
    }
    if (categoryExpanded) {
        ingredientsList.forEach { ingredient ->
            ShoppingItem2(
                ingredient,
                onCheckboxClicked = { onCheckboxClicked(it) },
                onDeleteShoppingItem = onDeleteShoppingItem
            )
        }
    }
}


@Composable
fun ShoppingItem2(
    ingredient: ShoppingIngredient,
    onCheckboxClicked: (ShoppingIngredient) -> Unit = {},
    onDeleteShoppingItem: (ShoppingIngredient) -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (ingredient.shoppingDone) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Checkbox(
            checked = ingredient.shoppingDone,
            onCheckedChange = { onCheckboxClicked(ingredient) },
            modifier = Modifier.padding(end = 8.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                disabledCheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                disabledUncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        )
        Text(
            modifier = Modifier.weight(1f),
            text = ingredient.toString(),
            color = if (ingredient.shoppingDone) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(
            onClick = {
                if (ingredient.nameEnteredByUser != "") {
                    showDeleteDialog = true
                } else {
                    showDialog = true
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            if (ingredient.nameEnteredByUser != "") {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Notiz hinzufügen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showDialog) {
            EditTextDialog(
                title = ingredient.note,
                onConfirm = {
                    ingredient.note = it
                    showDialog = false
                },
                onDismiss = { showDialog = false },
                label = "Notiz anpassen",
                initialValue = ingredient.note,
            )
        }
        if (showDeleteDialog) {
            ConfirmDialog(
                onConfirm = { onDeleteShoppingItem(ingredient) },
                onDismiss = { showDeleteDialog = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDayNavigationBar(
    multiDayShoppingList: MultiDayShoppingList,
    selectedDate: LocalDate?,
    currentState: ShoppingListState,
    onDateSelected: (LocalDate) -> Unit
) {
    val shoppingDays = multiDayShoppingList.getShoppingDaysInOrder()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            // Horizontal row of TopAppBar-style day items
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                shoppingDays.forEach { date ->
                    val isSelected = date == selectedDate

                    // Calculate completion from current state when this day is selected
                    val (completedCount, totalCount) = if (isSelected) {
                        val completed = currentState.ingredientsByCategory[shoppingDone]?.size ?: 0
                        val total = currentState.currentList.size
                        completed to total
                    } else {
                        // For non-selected days, use the stored daily list data
                        val dailyList = multiDayShoppingList.dailyLists[date]
                        val completed = dailyList?.getCompletedItemsCount() ?: 0
                        val total = dailyList?.ingredients?.size ?: 0
                        completed to total
                    }

                    Surface(
                        modifier = Modifier
                            .clickable { onDateSelected(date) }
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = HelperFunctions.formatDate(date),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            if (totalCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$completedCount/$totalCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}