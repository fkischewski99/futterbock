package view.event.categorized_shopping_list

import CategorizedShoppingListViewModel
import ShoppingListState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import model.ShoppingIngredient
import model.Source
import org.koin.compose.koinInject
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState

@Composable
fun ShoppingListScreen(navController: NavHostController) {
    val viewModelShoppingList: CategorizedShoppingListViewModel = koinInject()
    val state = viewModelShoppingList.state.collectAsStateWithLifecycle()

    ShoppingListCategorized(
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is EditShoppingListActions -> viewModelShoppingList.onAction(action)
            }
        },
        state = state.value
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListCategorized(
    onAction: (BaseAction) -> Unit,
    state: ResultState<ShoppingListState>,
) {
    var bottomSheetHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )
    // Inject view Model
    if (state is ResultState.Success) {
        BottomSheetWithSearchBar(
            items = state.data.currentList,
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

                    state.data.ingredientsByCategory.forEach { (category, ingredients) ->
                        ShoppingList(ingredients, category,
                            onCheckboxClicked = {
                                onAction(EditShoppingListActions.ToggleShoppingDone(it))
                            })
                    }
                }
            },
        )
    } else if (state is ResultState.Loading) {
        MGCircularProgressIndicator()
    } else {
        Text("Fehler beim abrufen der Einkaufsliste")
    }
}

@Composable
fun ShoppingList(
    ingredientsList: List<ShoppingIngredient>,
    category: String,
    onCheckboxClicked: (ShoppingIngredient) -> Unit = {}
) {
    var categoryExpanded by remember { mutableStateOf(true) }
    Spacer(modifier = Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth()
        .clickable { categoryExpanded = !categoryExpanded }
        .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = category + " (" + ingredientsList.size + ")",
            style = MaterialTheme.typography.titleMedium,
        )
        Icon(
            imageVector = if (categoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    if (categoryExpanded) {
        ingredientsList.forEach { ingredient ->
            ShoppingItem2(ingredient, onCheckboxClicked = { onCheckboxClicked(it) })
        }
    }
}


@Composable
fun ShoppingItem2(
    ingredient: ShoppingIngredient,
    onCheckboxClicked: (ShoppingIngredient) -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp, top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))

    ) {
        Checkbox(
            ingredient.shoppingDone,
            modifier = Modifier.weight(1f),
            onCheckedChange = { onCheckboxClicked(ingredient) },
            colors = CheckboxDefaults.colors(uncheckedColor = MaterialTheme.colorScheme.onPrimary)
        )
        Text(
            modifier = Modifier.weight(4f).padding(top = 4.dp, bottom = 4.dp),
            text = ingredient.toString(), color = MaterialTheme.colorScheme.onPrimary
        )
        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier.padding(end = 8.dp).weight(1f),
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Add note",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        if (showDialog) {
            EditNoteDialog(
                note = ingredient.note,
                onConfirm = {
                    ingredient.note = it
                    showDialog = false
                }, onDismiss = { showDialog = false })
        }
    }
}