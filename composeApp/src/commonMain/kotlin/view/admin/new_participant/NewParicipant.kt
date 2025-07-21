package view.admin.new_participant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import model.EatingHabit
import model.FoodIntolerance
import model.Ingredient
import org.koin.compose.koinInject
import view.event.categorized_shopping_list.IngredientViewModel
import view.event.new_meal_screen.AllParticipantsViewModel
import view.admin.new_participant.IngredientPickerDialog
import view.login.ErrorField
import view.shared.GroupSelector
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.date.DatePickerDialog
import view.shared.date.dateinputfield.DateInputField
import view.theme.AppTheme

@Composable
fun NewParticipantScreen(
    navController: NavHostController
) {
    val viewModelNewParticipant: ViewModelNewParticipant = koinInject()
    val state = viewModelNewParticipant.state.collectAsStateWithLifecycle()
    val ingredientViewModel: IngredientViewModel = koinInject();
    val allIngredientState = ingredientViewModel.state.collectAsStateWithLifecycle()
    val allIngredientList: List<Ingredient> = when (val state = allIngredientState.value) {
        is ResultState.Success -> state.data
        else -> emptyList()
    }
    val allParticipantsViewModel: AllParticipantsViewModel = koinInject()
    val allParticipantsState = allParticipantsViewModel.state.collectAsStateWithLifecycle()

    NewParicipant(
        state = state.value,
        ingredientList = allIngredientList,
        availableGroups = allParticipantsState.value.getSuccessData()?.availableGroups ?: setOf(),
        onAction = { action ->
            when (action) {
                is ActionsNewParticipant.GoBack -> {
                    navController.navigateUp()
                    viewModelNewParticipant.onAction(action)
                }

                else -> {
                    viewModelNewParticipant.onAction(action)
                }
            }
        }
    )

}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewParicipant(
    state: ResultState<NewParticipantState>,
    onAction: (ActionsNewParticipant) -> Unit,
    ingredientList: List<Ingredient>,
    availableGroups: Set<String>
) {

    AppTheme {
        Scaffold(
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(title = {
                    Text(text = "Teilnehmende hinzufügen")
                }, navigationIcon = {
                    NavigationIconButton(onLeave = { onAction(ActionsNewParticipant.GoBack) })
                })
            }
        ) {
            Column(
                modifier = Modifier.padding(it).verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                when (state) {
                    is ResultState.Success -> {

                        Row {
                            OutlinedTextField(
                                value = state.data.firstName,
                                onValueChange = {
                                    onAction(ActionsNewParticipant.ChangeFirstName(it))
                                },
                                label = { Text("Vorname:") },
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Row {
                            OutlinedTextField(
                                value = state.data.lastName,
                                onValueChange = { onAction(ActionsNewParticipant.ChangeLastName(it)) },
                                label = { Text("Nachname:") },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Row {
                            DropDownEatingHabit(
                                state = state.data, onAction = onAction
                            )
                        }
                        Row {
                            GroupSelector(
                                selectedGroup = state.data.selectedGroup,
                                availableGroups = availableGroups,
                                onGroupSelected = { group ->
                                    onAction(ActionsNewParticipant.SelectGroup(group))
                                }
                            )
                        }
                        Row {
                            BirthdaySelectionField(state, onAction)

                        }
                        IntoleranceSelection(
                            state = state.data,
                            onAction = onAction
                        )
                        AllergySelection(
                            state = state.data,
                            onAction = onAction,
                            ingredientList = ingredientList
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                        ) {
                            Button(
                                // Calendar icon to open DatePicker
                                onClick = {
                                    onAction(ActionsNewParticipant.Save)
                                },
                                modifier = Modifier
                                    .padding(16.dp).size(width = 200.dp, height = 60.dp),


                                ) {
                                Text("Speichern")

                            }
                        }
                        if (state.data.showDatePicker) {
                            DatePickerDialog(
                                onSelect = { millis ->
                                    onAction(ActionsNewParticipant.SelectBirthDate(millis))
                                }
                            )
                        }
                    }

                    is ResultState.Error -> ErrorField(errorMessage = state.message)
                    ResultState.Loading -> MGCircularProgressIndicator()
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergySelection(
    state: NewParticipantState,
    onAction: (ActionsNewParticipant) -> Unit,
    ingredientList: List<Ingredient>
) {
    var showDialog by remember { mutableStateOf(false) }
    val ingredientMap = remember(ingredientList) {
        ingredientList.associateBy { it.uid }
    }

    Row(
        modifier = Modifier.padding(8.dp).clickable { showDialog = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Allergien:", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { showDialog = true }) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Allergien bearbeiten"
            )
        }
    }
    // Display selected allergies (you can customize this part)
    FlowRow(modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth()) {
        state.allergies
            .mapNotNull { allergy -> ingredientMap[allergy] }
            .forEach { ingredient ->
                FilterChip(
                    selected = true,
                    onClick = { showDialog = true },
                    label = { Text(ingredient.name) },
                    modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                )
            }
    }

    if (showDialog) {
        IngredientPickerDialog(
            ingredientList = ingredientList,
            selectedIngredients = state.allergies,
            onSelected = { onAction(ActionsNewParticipant.AddOrRemoveAllergy(allergy = it.uid)) },
            onDismiss = { showDialog = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntoleranceSelection(
    state: NewParticipantState,
    onAction: (ActionsNewParticipant) -> Unit
) {
    Row(modifier = Modifier.padding(8.dp)) {
        Text("Unverträglichkeiten:", style = MaterialTheme.typography.titleMedium)
    }
    FlowRow(
        modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth()
    ) {
        FilterChip(
            onClick = { onAction(ActionsNewParticipant.AddOrRemoveIntolerance(FoodIntolerance.GLUTEN_INTOLERANCE)) },
            label = { Text("Glutenfrei") },
            selected = state.foodIntolerance.contains(FoodIntolerance.GLUTEN_INTOLERANCE),
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        )
        FilterChip(
            onClick = { onAction(ActionsNewParticipant.AddOrRemoveIntolerance(FoodIntolerance.LACTOSE_INTOLERANCE)) },
            label = { Text("Laktosefrei") },
            selected = state.foodIntolerance.contains(FoodIntolerance.LACTOSE_INTOLERANCE),
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        )
        FilterChip(
            onClick = { onAction(ActionsNewParticipant.AddOrRemoveIntolerance(FoodIntolerance.FRUCTOSE_INTOLERANCE)) },
            label = { Text("Fruktosefrei") },
            selected = state.foodIntolerance.contains(FoodIntolerance.FRUCTOSE_INTOLERANCE),
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        )
        FilterChip(
            onClick = { onAction(ActionsNewParticipant.AddOrRemoveIntolerance(FoodIntolerance.WITHOUT_NUTS)) },
            label = { Text("Ohne Nüsse") },
            selected = state.foodIntolerance.contains(FoodIntolerance.WITHOUT_NUTS),
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun BirthdaySelectionField(
    state: ResultState.Success<NewParticipantState>,
    onAction: (ActionsNewParticipant) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    if (interactionSource.collectIsPressedAsState().value) {
        onAction(ActionsNewParticipant.ShowDatePicker)
    }

    DateInputField(
        date = state.data.birthDate,
        label = "Geburtsdatum:",
        trailingIcon = {
            IconButton(
                onClick = {
                    onAction(ActionsNewParticipant.ShowDatePicker)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Bearbeiten",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownEatingHabit(
    state: NewParticipantState,
    onAction: (ActionsNewParticipant) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth().padding(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            TextField(
                label = { Text("Ernährungsweise:") },
                value = state.selectedHabit.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true)
                    .background(color = Color.Transparent)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                EatingHabit.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item.name) },
                        onClick = {
                            onAction(ActionsNewParticipant.SelectEatingHabit(item))
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

