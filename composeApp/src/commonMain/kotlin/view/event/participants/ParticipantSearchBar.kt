package view.event.participants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
import org.koin.compose.koinInject
import view.admin.new_participant.ActionsNewParticipant
import view.admin.new_participant.ViewModelNewParticipant
import view.event.EventState
import view.event.SharedEventViewModel
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
    allParticipants: ResultState<AllParticipantsState>
) {
    var searchText by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }


    Scaffold(
        topBar = {
            NavigationIconButton()
            when (state) {
                is ResultState.Success -> {

                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchText,
                                onQueryChange = { searchText = it },
                                onSearch = {
                                    active = false
                                    // Optional: handle search
                                },
                                expanded = active,
                                onExpandedChange = { active = it },
                                enabled = true,
                                placeholder = { Text("Teilnehmende hinzufügen") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Suche"
                                    )
                                },
                                trailingIcon = {
                                    if (active) {
                                        IconButton(onClick = {
                                            if (searchText.isEmpty()) {
                                                onAction(NavigationActions.GoBack)
                                            } else {
                                                searchText = ""
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Leeren oder Zurück"
                                            )
                                        }
                                    }
                                },
                                colors = SearchBarDefaults.inputFieldColors(),
                                interactionSource = remember { MutableInteractionSource() }
                            )
                        },
                        expanded = active,
                        onExpandedChange = { active = it },
                        modifier = Modifier.fillMaxWidth(),
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
                                            Text(text = it.getListItemTitle())
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
                        when (allParticipants) {
                            is ResultState.Success -> {
                                getSelectableParticipants(
                                    allParticipants = allParticipants.data.allParticipants,
                                    participantsOfEvent = state.data.participantList
                                ).filter {
                                    it.firstName.lowercase().contains(searchText.lowercase()) ||
                                            it.lastName.lowercase()
                                                .contains(searchText.lowercase()) ||
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
                            }

                            else -> {}
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
                                        onAction(NavigationActions.GoToRoute(Routes.CreateOrEditParticipant))
                                    },
                                    modifier = Modifier.padding(bottom = 16.dp)
                                        .width(400.dp)
                                        .clip(shape = RoundedCornerShape(75)), // Limit the width to prevent stretching,
                                    containerColor = MaterialTheme.colorScheme.onPrimary,

                                    ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Icon"
                                        )
                                        Text(
                                            text = "Teilnehmende anlegen",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        onAction(NavigationActions.GoBack)
                                    },
                                    modifier = Modifier.padding(bottom = 16.dp)
                                        .width(400.dp)
                                        .clip(shape = RoundedCornerShape(75)), // Limit the width to prevent stretching,
                                    elevation = FloatingActionButtonDefaults.elevation(16.dp),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Add Icon"
                                        )
                                        Text(
                                            text = "Teilnehmende übernehmen",
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    //elevation = AppBarDefaults.TopAppBarElevation

                }

                is ResultState.Error -> ErrorField(errorMessage = state.message)
                ResultState.Loading -> MGCircularProgressIndicator()
            }
        }
    ) {

    }

}