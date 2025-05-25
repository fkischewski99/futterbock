package view.event.new_event

import CardWithList
import CategorizedShoppingListViewModel
import MaterialListViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import getPlatformName
import kotlinx.coroutines.delay
import model.Event
import org.koin.compose.koinInject
import services.event.eventIsEditable
import view.event.EventState
import view.event.SharedEventViewModel
import view.event.actions.BaseAction
import view.event.actions.EditEventActions
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.categorized_shopping_list.EditShoppingListActions
import view.event.materiallist.EditMaterialListActions
import view.login.ErrorField
import view.navigation.Routes
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.page.ColumnWithPadding

@Composable
fun NewEventScreen(
    navController: NavHostController,
) {
    val sharedEventViewModel: SharedEventViewModel = koinInject()
    val shoppingIngredient: CategorizedShoppingListViewModel = koinInject()
    val materialList: MaterialListViewModel = koinInject()
    val sharedState = sharedEventViewModel.eventState.collectAsStateWithLifecycle()

    NewEventPage(
        sharedState = sharedState.value,
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is EditEventActions -> sharedEventViewModel.onAction(action)
                is EditShoppingListActions.Initialize -> shoppingIngredient.onAction(
                    action
                )

                is EditMaterialListActions -> materialList.onAction(action)
            }
        }
    )
}

@Composable
fun NewEventPage(
    sharedState: ResultState<EventState>,
    onAction: (BaseAction) -> Unit

) {
    Scaffold(topBar = {
        topBarEventPage(
            onAction = onAction,
            sharedState = sharedState
        )
    }) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize().padding(top = it.calculateTopPadding())
        ) {
            when (sharedState) {
                is ResultState.Success -> {
                    var eventName by remember { mutableStateOf(sharedState.data.event.name) }

                    Column(
                        modifier = Modifier.padding(8.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = eventName,
                                onValueChange = { value ->
                                    eventName = value
                                    onAction(EditEventActions.ChangeEventName(value))
                                },
                                label = { Text("Name:") },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ParticipantNumberTextField(sharedState, onAction)
                        }
                        SimpleDateRangePickerInDatePickerDialog(
                            from = sharedState.data.event.from,
                            to = sharedState.data.event.to,
                            onSelect = { start, end ->
                                if (eventIsEditable(sharedState.data.event.to)) {
                                    onAction(EditEventActions.ChangeEventDates(start, end))
                                } else {
                                    onAction(EditEventActions.CopyEventToFuture(start, end))
                                }
                            },
                            isEditable = true,
                            buttonText = getButtonText(sharedState.data.event)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        sharedState.data.mealsGroupedByDate.forEach { it ->
                            val date = it.key
                            CardWithList(
                                title = "" + date.dayOfMonth + "." + date.month,
                                listItems = it.value,
                                onListItemClick = { item ->
                                    onAction(EditEventActions.EditExistingMeal(item.getItem()))
                                    onAction(NavigationActions.GoToRoute(Routes.EditMeal))
                                },
                                addItemToList = {
                                    onAction(EditEventActions.AddNewMeal(it.key))
                                    onAction(NavigationActions.GoToRoute(Routes.EditMeal))
                                },
                                onDeleteClick = { onAction(EditEventActions.DeleteMeal(it.getItem())) })
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ShoppingAndMaterialList(onAction, sharedState)
                    }
                }

                is ResultState.Loading -> {
                    ColumnWithPadding { MGCircularProgressIndicator() }
                }

                is ResultState.Error -> {
                    ColumnWithPadding { ErrorField(sharedState.message) }
                }
            }
        }
    }
}

@Composable
private fun ParticipantNumberTextField(
    sharedState: ResultState.Success<EventState>,
    onAction: (BaseAction) -> Unit
) {
    val source = remember {
        MutableInteractionSource(

        )
    }

    if (source.collectIsPressedAsState().value) {
        onAction(
            NavigationActions.GoToRoute(
                Routes.ParticipantsOfEvent
            )
        )
    }

    OutlinedTextField(
        value = "" + sharedState.data.participantList.size,
        readOnly = true,
        onValueChange = { },
        interactionSource = source,
        label = { Text("Teilnehmendenanzahl:") },
        modifier = Modifier.padding(8.dp),
        trailingIcon = {
            TextButton(
                // Calendar icon to open DatePicker
                onClick = {
                    onAction(
                        NavigationActions.GoToRoute(
                            Routes.ParticipantsOfEvent
                        )
                    )
                },
                modifier = Modifier
                    .padding(8.dp).height(IntrinsicSize.Min)
                    .clip(shape = RoundedCornerShape(75))
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Person",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Person",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    )
}

@Composable
private fun ShoppingAndMaterialList(
    onAction: (BaseAction) -> Unit,
    sharedState: ResultState.Success<EventState>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp)
    ) {
        Button(
            onClick = {
                onAction(EditShoppingListActions.Initialize(sharedState.data.event.uid))
                onAction(
                    NavigationActions.GoToRoute(
                        Routes.ShoppingList(sharedState.data.event.uid)
                    )
                )
            },
            modifier = Modifier.padding(8.dp).height(IntrinsicSize.Min)
                .align(Alignment.CenterVertically),
            colors = ButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),

            ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Add Icon",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Einkaufsliste")
        }
        Button(
            onClick = {
                onAction(EditMaterialListActions.Initialize(sharedState.data.event.uid))
                onAction(
                    NavigationActions.GoToRoute(
                        Routes.MaterialList
                    )
                )
            },
            modifier = Modifier.padding(8.dp).height(IntrinsicSize.Min)
                .align(Alignment.CenterVertically),
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            ),
            colors = ButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.primaryContainer
            ),

            ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Add Icon",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Materialliste")
        }
    }
}


fun getButtonText(event: Event): String {
    if (eventIsEditable(event.to)) {
        return "Datum ändern"
    }
    return "In aktuelles Event kopieren"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topBarEventPage(
    onAction: (BaseAction) -> Unit,
    sharedState: ResultState<EventState>
) {

    TopAppBar(title = {
        Text(text = "Lager bearbeiten")
    }, navigationIcon = {
        NavigationIconButton(onLeave = {
            onAction(NavigationActions.GoBack)
            onAction(EditEventActions.SaveEvent)
        })
    }, actions = {
        Row(
            horizontalArrangement = Arrangement.End,
        ) {

            IconButton(
                onClick = {
                    if (sharedState is ResultState.Success) {
                        onAction(EditShoppingListActions.Initialize(sharedState.data.event.uid))
                        onAction(
                            NavigationActions.GoToRoute(
                                Routes.ShoppingList(sharedState.data.event.uid)
                            )
                        )
                    }
                },
                modifier = Modifier.clip(shape = RoundedCornerShape(75))
                    .background(MaterialTheme.colorScheme.tertiary),

                ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Shopping Cart Icon",
                    tint = MaterialTheme.colorScheme.onTertiary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            sharePdfButton(onAction)
        }
    })

}

@Composable
private fun sharePdfButton(onAction: (BaseAction) -> Unit) {
    var isButtonEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(isButtonEnabled) {
        if (isButtonEnabled) return@LaunchedEffect
        else delay(2000L)
        isButtonEnabled = true
    }

    IconButton(
        onClick = {
            if (isButtonEnabled) {
                isButtonEnabled = false
                onAction(EditEventActions.SharePdf)
            }
        },
        enabled = isButtonEnabled,
        modifier = Modifier.clip(shape = RoundedCornerShape(75))
            .background(MaterialTheme.colorScheme.tertiary),
    ) {
        val imageVector = when (getPlatformName()) {
            "desktop" -> Icons.Default.Save
            else -> Icons.Default.Share
        }
        Icon(
            imageVector = imageVector,
            contentDescription = "Printer Icon",
            tint = MaterialTheme.colorScheme.onTertiary
        )

    }
}



