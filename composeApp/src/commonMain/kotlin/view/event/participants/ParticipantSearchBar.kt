package view.event.participants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
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
import model.Participant
import org.koin.compose.koinInject
import view.admin.new_participant.ActionsNewParticipant
import view.admin.new_participant.ViewModelNewParticipant
import view.event.EventState
import view.event.SharedEventViewModel
import view.event.actions.BaseAction
import view.event.actions.EditEventActions
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.new_meal_screen.AllParticipantsViewModel
import view.navigation.Routes
import view.shared.NavigationIconButton
import view.shared.ResultState

@Composable
fun ParticipantSearchBarScreen(
    navController: NavHostController
) {
    val sharedEventViewModel: SharedEventViewModel = koinInject()
    val state = sharedEventViewModel.eventState.collectAsStateWithLifecycle()
    val allParticipantsViewModel = koinInject<AllParticipantsViewModel>()
    val allParticipants = allParticipantsViewModel.state.collectAsStateWithLifecycle()
    val viewModelNewParticipant: ViewModelNewParticipant = koinInject()

    ParticipantSearchBar(
        allParticipants = allParticipants.value,
        state = state.value,
        onAction = { action ->
            when (action) {
                is ActionsNewParticipant.InitWithoutParticipant -> viewModelNewParticipant.onAction(
                    action
                )

                is NavigationActions -> handleNavigation(navController, action)
                is EditParticipantActions -> sharedEventViewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParticipantSearchBar(
    state: ResultState<EventState>,
    onAction: (BaseAction) -> Unit,
    allParticipants: List<Participant>
) {
    var searchText by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }


    Scaffold(
        topBar = {
            NavigationIconButton()
            when (state) {
                is ResultState.Success -> {
                    SearchBar(
                        modifier = Modifier.fillMaxWidth(),
                        query = searchText,
                        placeholder = { Text(text = "Teilnehmer hinzufügen") },
                        onQueryChange = {
                            searchText = it
                        },
                        onSearch = {
                            active = false
                        },
                        active = active,
                        onActiveChange = {
                            active = it
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Suche")
                        },
                        trailingIcon = {
                            if (active) {
                                Icon(
                                    modifier = Modifier.clickable {
                                        if (searchText.isEmpty()) {
                                            onAction(NavigationActions.GoBack)
                                        } else {
                                            searchText = ""
                                        }

                                    },
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            modifier = Modifier
                                .padding(1.dp),
                        ) {
                            state.data.participantList.forEach {
                                InputChip(
                                    label = {
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            Text(text = it.getTitle())
                                        }
                                    },
                                    onClick = {
                                        onAction(
                                            EditParticipantActions.DeleteParticipant(
                                                it
                                            )
                                        )
                                    },
                                    selected = true,
                                    modifier = Modifier.padding(start = 4.dp),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .clickable {
                                                    onAction(
                                                        EditParticipantActions.DeleteParticipant(
                                                            it
                                                        )
                                                    )
                                                }
                                        )
                                    }

                                )
                            }
                        }
                        getSelectableParticipants(
                            allParticipants = allParticipants,
                            participantsOfEvent = state.data.participantList
                        ).filter {
                            it.firstName.lowercase().contains(searchText.lowercase()) ||
                                    it.lastName.lowercase().contains(searchText.lowercase()) ||
                                    (it.firstName.lowercase() + " " + it.lastName.lowercase()).contains(
                                        searchText.lowercase()
                                    )
                        }.forEach {
                            Row(
                                modifier = Modifier.padding(16.dp).clickable {
                                    searchText = ""
                                    onAction(EditParticipantActions.AddParticipant(it))
                                }

                            ) {
                                Text(text = it.firstName.trim() + " " + it.lastName.trim())
                            }
                            HorizontalDivider()
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                            ) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        onAction(ActionsNewParticipant.InitWithoutParticipant)
                                        onAction(NavigationActions.GoToRoute(Routes.CreateNewParticipant))
                                    },
                                    modifier = Modifier.padding(bottom = 16.dp)
                                        .clip(shape = RoundedCornerShape(75)), // Limit the width to prevent stretching,
                                    containerColor = MaterialTheme.colorScheme.onPrimary,

                                    ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Icon"
                                    )
                                    Text("Teilnehmer anlegen       ")
                                }
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        onAction(NavigationActions.GoBack)
                                    },
                                    modifier = Modifier.padding(bottom = 16.dp)
                                        .clip(shape = RoundedCornerShape(75)), // Limit the width to prevent stretching,
                                    elevation = FloatingActionButtonDefaults.elevation(16.dp),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Add Icon"
                                    )
                                    Text("Teilnehmer übernehmen")
                                }
                            }
                        }
                    }

                    //elevation = AppBarDefaults.TopAppBarElevation

                }

                is ResultState.Error -> TODO()
                ResultState.Loading -> TODO()
            }
        }
    ) {

    }

}