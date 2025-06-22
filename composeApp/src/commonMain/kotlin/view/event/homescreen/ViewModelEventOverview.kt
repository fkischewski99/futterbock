package view.event.homescreen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import model.Event
import services.login.LoginAndRegister
import view.shared.ResultState


data class EventOverviewState(
    val pastEvents: List<Event> = ArrayList(),
    val upcommingEvents: List<Event> = ArrayList(),
)

class ViewModelEventOverview(
    private val eventRepository: EventRepository,
    private val loginAndRegister: LoginAndRegister
) : ViewModel() {

    private var _state = MutableStateFlow<ResultState<EventOverviewState>>(ResultState.Loading)
    val state = _state.asStateFlow()
    
    init {
        // Initialize the Flow in a coroutine
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userGroup = loginAndRegister.getCustomUserGroup()
                eventRepository.getEventList(userGroup)
                    .collect { events -> 
                        try {
                            Logger.i("Processing ${events.size} events")
                            val (pastEvents, upcomingEvents) = events.partition { !it.isFutureEvent() }
                            _state.value = ResultState.Success(
                                EventOverviewState(
                                    pastEvents = pastEvents,
                                    upcommingEvents = upcomingEvents
                                )
                            )
                        } catch (e: Exception) {
                            Logger.e("Error processing events: ${e.message}")
                            _state.value = ResultState.Error("Fehler beim Verarbeiten der Lager")
                        }
                    }
            } catch (e: Exception) {
                Logger.e("Error initializing event flow: ${e.message}")
                _state.value = ResultState.Error("Fehler beim Initialisieren der Lager")
            }
        }
    }

    var data = mutableStateOf(emptyList<Event>())

    fun onAction(actionsEventOverview: ActionsEventOverview) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (actionsEventOverview) {
                    is ActionsEventOverview.DeleteEvent -> onDeleteClick(actionsEventOverview.eventId)
                    ActionsEventOverview.Init -> {
                        // No longer needed - Flow is automatically managed
                        Logger.i("ViewModel initialized - Flow will handle data updates")
                    }
                    else -> {
                        //only navigation
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error handling action: " + e.stackTraceToString())
            }
        }
    }

    fun onDeleteClick(eventId: String) {
        // Optimistic UI update is now handled by the reactive Flow
        // Just delete from DB and the Flow will automatically update the UI
        viewModelScope.launch(Dispatchers.IO) { 
            try {
                eventRepository.deleteEvent(eventId)
                Logger.i("Deleted event: $eventId")
            } catch (e: Exception) {
                Logger.e("Error deleting event: ${e.message}")
                // Consider showing error message to user
            }
        }
    }
}