package view.event.homescreen

import CardWithList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import view.event.SharedEventViewModel
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.navigation.Routes
import view.shared.MGCircularProgressIndicator
import view.shared.ResultState

@Composable
fun EventOverviewScreen(
    navController: NavHostController
) {
    val viewModel: ViewModelEventOverview = koinInject()
    val viewModeNewEvent: SharedEventViewModel = koinInject()
    val state = viewModel.state.collectAsStateWithLifecycle()

    EventOverview(
        state = state.value,
        onAction = { action ->
            when (action) {
                is ActionsEventOverview.EditEvent -> {
                    viewModeNewEvent.initializeScreen(action.eventId)
                    navController.navigate(Routes.EditEvent)
                }

                is ActionsEventOverview.Logout -> navController.navigate(Routes.Login)
                is ActionsEventOverview.NewEvent -> {
                    viewModeNewEvent.initializeScreen(null)
                    navController.navigate(Routes.EditEvent)
                }

                is NavigationActions -> handleNavigation(navController, action)
                is ActionsEventOverview -> viewModel.onAction(action)
                else -> {
                    Logger.w("Unknown action: $action")
                }
            }
        }
    )
}

@Composable
fun EventOverview(
    state: ResultState<EventOverviewState>,
    onAction: (BaseAction) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onAction(ActionsEventOverview.Init)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onClose = { scope.launch { drawerState.close() } },
                onLogoutNavigation = { onAction(ActionsEventOverview.Logout) },
                onManageParticipants = { onAction(NavigationActions.GoToRoute(Routes.ParticipantAdministration)) },
                onManageRecipes = { onAction(NavigationActions.GoToRoute(Routes.RecipeManagement)) }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                EventOverviewTopBar {
                    scope.launch {
                        drawerState.apply {
                            if (isClosed) open() else close()
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())
            ) {
                EventOverviewContent(
                    state = state,
                    onAction = onAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventOverviewTopBar(
    onAction: (BaseAction) -> Unit
) {
    TopAppBar(
        title = { Text(text = "Lagerübersicht") },
        actions = {
            IconButton(onClick = { onAction(NavigationActions.GoToRoute(Routes.ParticipantsOfEvent)) }) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "User Icon"
                )
            }
        }
    )
}

@Composable
fun EventOverviewContent(
    state: ResultState<EventOverviewState>,
    onAction: (ActionsEventOverview) -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        when (state) {
            is ResultState.Success -> {
                CardWithList(
                    title = "Aktuelle Lager",
                    listItems = state.data.upcommingEvents.sortedByDescending { it.from },
                    addItemToList = { onAction(ActionsEventOverview.NewEvent) },
                    onDeleteClick = { onAction(ActionsEventOverview.DeleteEvent(it.getItem().uid)) },
                    onListItemClick = { onAction(ActionsEventOverview.EditEvent(it.getItem().uid)) }
                )
                CardWithList(
                    title = "Vergangene Lager",
                    listItems = state.data.pastEvents.sortedByDescending { it.from },
                    onListItemClick = { onAction(ActionsEventOverview.EditEvent(it.getItem().uid)) },
                    onDeleteClick = { onAction(ActionsEventOverview.DeleteEvent(it.getItem().uid)) },
                )
            }

            ResultState.Loading -> {
                MGCircularProgressIndicator()
            }

            is ResultState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
