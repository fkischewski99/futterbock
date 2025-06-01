package view.admin.participants

import CardWithList
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.koin.compose.koinInject
import view.admin.new_participant.ActionsNewParticipant
import view.admin.new_participant.ViewModelNewParticipant
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.new_meal_screen.AllParticipantsState
import view.event.new_meal_screen.AllParticipantsViewModel
import view.login.ErrorField
import view.navigation.Routes
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState


@Composable
fun ParticipantAdminScreen(
    navController: NavHostController
) {
    val allParticipantsViewModel: AllParticipantsViewModel = koinInject()
    val state = allParticipantsViewModel.state.collectAsStateWithLifecycle()

    val viewModelNewParticipant = koinInject<ViewModelNewParticipant>()

    ParticipantPage(
        state = state.value,
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is ActionsNewParticipant -> viewModelNewParticipant.onAction(action)
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantPage(
    state: ResultState<AllParticipantsState>,
    onAction: (BaseAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stammesmitglieder") },
                navigationIcon = {
                    NavigationIconButton(
                        onLeave = {
                            onAction(NavigationActions.GoBack)
                        }
                    )
                }
            )
        }
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = it.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (state) {
                is ResultState.Success -> {

                    Column(
                        modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())
                    ) {
                        CardWithList(
                            title = "Teilnehmende",
                            listItems = state.data.allParticipants,
                            onListItemClick = { item ->
                                onAction(ActionsNewParticipant.InitWithParticipant(item.getItem()))
                                onAction(NavigationActions.GoToRoute(Routes.CreateOrEditParticipant))
                            },
                            addItemToList = {
                                onAction(ActionsNewParticipant.InitWithoutParticipant)
                                onAction(NavigationActions.GoToRoute(Routes.CreateOrEditParticipant))
                            },
                            onDeleteClick = { participant ->
                                onAction(ActionsNewParticipant.DeleteParticipant(participant.getItem().uid))
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

