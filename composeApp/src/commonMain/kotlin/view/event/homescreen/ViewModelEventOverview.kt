package view.event.homescreen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    var data = mutableStateOf(emptyList<Event>())

    fun onAction(actionsEventOverview: ActionsEventOverview) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                when (actionsEventOverview) {
                    is ActionsEventOverview.DeleteEvent -> onDeleteClick(actionsEventOverview.eventId)
                    ActionsEventOverview.Init -> initializeScreen()
                    else -> {
                        //only navigagion
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error when loading events: " + e.stackTraceToString())
                _state.value = ResultState.Error("Fehler beim Abrufen der Lager")
            }
        }
    }

    private fun sortEventsByDate(eventList: List<Event>) {
        val upcommingEvents = ArrayList<Event>()
        val pastEvents = ArrayList<Event>()
        for (event in eventList) {
            if (event.isFutureEvent()) {
                upcommingEvents.add(event)
            } else {
                pastEvents.add(event)
            }
        }
        _state.value = ResultState.Success(
            EventOverviewState(
                pastEvents = pastEvents,
                upcommingEvents = upcommingEvents
            )
        )
        Logger.i("Sorted Events")


    }

    fun onDeleteClick(eventId: String) {
        val data = _state.value.getSuccessData() ?: return;
        var upcommingEvents = data.upcommingEvents.filter { it.uid !== eventId }
        val copy = _state.value.getSuccessData()!!.copy(
            upcommingEvents = upcommingEvents
        )
        _state.value = ResultState.Success(copy)
        //Delete from DB
        viewModelScope.launch(Dispatchers.IO) { eventRepository.deleteEvent(eventId); }
    }

    private suspend fun initializeScreen() {
        Logger.i("Init viewmodel EventOverview")


        val list = eventRepository.getEventList(loginAndRegister.getCustomUserGroup())
        Logger.i("Get Events was sucessfull")
        list.collect { listOfEvents ->
            sortEventsByDate(listOfEvents)
        }
    }
}