package view.event.participants

import CardWithList
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import model.ParticipantTime
import org.koin.compose.koinInject
import view.event.EventState
import view.event.SharedEventViewModel
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.navigation.Routes
import view.shared.date.DateRangePickerDialog
import view.shared.HelperFunctions
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.date.SelectedAbleDatesWithFromTo


@Composable
fun ParticipantScreen(
    navController: NavHostController
) {
    val sharedEventViewModel: SharedEventViewModel = koinInject()
    val state = sharedEventViewModel.eventState.collectAsStateWithLifecycle()

    ParticipantPage(
        state = state.value,
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                else -> sharedEventViewModel.onAction(action)
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantPage(
    state: ResultState<EventState>,
    onAction: (BaseAction) -> Unit
) {

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedParticipant: ParticipantTime? = null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teilnehmendeliste") },
                navigationIcon = {
                    NavigationIconButton(
                        onLeave = {
                            onAction(NavigationActions.GoBack)
                            onAction(EditParticipantActions.UpdateAllMeals)
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
                        // CSV Import Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onAction(NavigationActions.GoToRoute(Routes.EventCsvImport(state.data.event.uid)))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("CSV Import")
                            }
                        }

                        CardWithList(
                            title = "Teilnehmende",
                            listItems = state.data.participantList.sortedBy { it.participant?.firstName },
                            onListItemClick = { item ->
                                showDatePicker = true
                                selectedParticipant = item.getItem()
                            },
                            addItemToList = {
                                onAction(NavigationActions.GoToRoute(Routes.AddOrRemoveParticipantsOfEvent))
                            },
                            onDeleteClick = { participant ->
                                onAction(EditParticipantActions.DeleteParticipant(participant.getItem()))
                            },
                            addItemToListAtTop = true

                        )
                    }

                    if (showDatePicker) {
                        DateRangePickerDialog(
                            onSelect = { startMillis, endMillis ->
                                showDatePicker = false
                                onAction(
                                    EditParticipantActions.SelectDateOfParticipant(
                                        selectedParticipant,
                                        startMillis,
                                        endMillis
                                    )
                                )
                            },
                            startMillis = HelperFunctions.getMillisInUTC(state.data.event.from),
                            endMillis = HelperFunctions.getMillisInUTC(state.data.event.to),
                            onDismiss = {
                                showDatePicker = false
                                selectedParticipant = null
                            },
                            selectableDates = SelectedAbleDatesWithFromTo(
                                state.data.event.from,
                                state.data.event.to
                            )
                        )
                    }
                }

                is ResultState.Error -> TODO()
                ResultState.Loading -> MGCircularProgressIndicator()
            }
        }
    }
}

