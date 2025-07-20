package view.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import model.Event
import model.Meal
import model.ParticipantTime
import services.ChangeDateOfEvent
import services.pdfService.PdfServiceModule
import view.event.actions.BaseAction
import view.event.actions.EditEventActions
import view.event.actions.LoadingAction
import view.event.new_event.HandleEditEvent
import view.event.new_event.groupMealsByDate
import view.event.new_meal_screen.EditMealActions
import view.event.new_meal_screen.HandleEditMealActions
import view.event.new_meal_screen.generateDateRange
import view.event.participants.EditParticipantActions
import view.event.participants.HandleParticipantsActions
import view.event.cooking_groups.CookingGroupActions
import view.event.cooking_groups.HandleCookingGroupActions
import view.shared.ResultState

data class EventState(
    val event: Event = Event(""),
    val mealList: List<Meal> = listOf(),
    val participantList: List<ParticipantTime> = listOf(),
    val mealsGroupedByDate: Map<LocalDate, List<Meal>> = mapOf(),
    val selectedMeal: Meal,
    val currentParticipantsOfMeal: List<ParticipantTime>,
    val dateRange: List<LocalDate>
)

class SharedEventViewModel(
    private val eventRepository: EventRepository,
    changeDateOfEvent: ChangeDateOfEvent,
    private val pdfServiceModule: PdfServiceModule
) : ViewModel() {
    // Initialize Shared view Model with empty event
    private var _eventState = MutableStateFlow<ResultState<EventState>>(ResultState.Loading)
    val eventState = _eventState.asStateFlow()
    private val handleEditEvent =
        HandleEditEvent(
            eventRepository = eventRepository,
            pdfServiceModule = pdfServiceModule,
            changeDateOfEvent = changeDateOfEvent
        )
    private val handleEditMeal =
        HandleEditMealActions(
            eventRepository = eventRepository,
        )
    private val handleEditParticipant =
        HandleParticipantsActions(
            eventRepository = eventRepository,
        )
    private val handleCookingGroups =
        HandleCookingGroupActions(
            eventRepository = eventRepository
        )


    fun onAction(editEventActions: BaseAction) {
        when (editEventActions) {
            is EditMealActions -> handleEditMealActions(editEventActions)
            is EditEventActions -> handleEventActions(editEventActions)
            is EditParticipantActions -> handleEditParticipantActions(editEventActions)
            is CookingGroupActions -> handleCookingGroupActions(editEventActions)
        }
    }

    private fun handleEventActions(editEventActions: EditEventActions) {
        val currentState = eventState.value.getSuccessData() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (editEventActions is LoadingAction)
                _eventState.value =
                    ResultState.Loading

            _eventState.value =
                handleEditEvent.handleAction(currentState, editEventActions)
        }
    }

    private fun handleEditMealActions(editEventActions: EditMealActions) {
        val currentState = eventState.value.getSuccessData() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _eventState.value =
                handleEditMeal.handleAction(currentState, editEventActions)
        }
    }

    private fun handleEditParticipantActions(editEventActions: EditParticipantActions) {
        val currentState = eventState.value.getSuccessData() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _eventState.value =
                handleEditParticipant.handleAction(currentState, editEventActions)
        }
    }

    private fun handleCookingGroupActions(action: CookingGroupActions) {
        val currentState = eventState.value.getSuccessData() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _eventState.value =
                handleCookingGroups.handleAction(currentState, action)
        }
    }


    fun initializeScreen(eventIdPrm: String?) {
        _eventState.value = ResultState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val eventFromRepo =
                    if (eventIdPrm == null) eventRepository.createNewEvent() else eventRepository.getEventById(
                        eventIdPrm
                    )
                if (eventFromRepo == null) {
                    _eventState.value = ResultState.Error("Fehler beim Abrufen des Lagers")
                    return@launch
                }
                val eventId = eventFromRepo.uid

                val participantListDeferred = async {
                    eventRepository.getParticipantsOfEvent(
                        eventId = eventId,
                        withParticipant = true
                    )
                }
                val allMealsOfEventDeferred = async {
                    eventRepository.getAllMealsOfEvent(eventId)
                }

                // Wait for both operations to complete
                val participantList = participantListDeferred.await()
                val allMealsOfEvent = allMealsOfEventDeferred.await()

                _eventState.value =
                    ResultState.Success(
                        EventState(
                            event = eventFromRepo,
                            mealList = allMealsOfEvent,
                            mealsGroupedByDate = groupMealsByDate(
                                eventFromRepo.from,
                                eventFromRepo.to,
                                allMealsOfEvent
                            ),
                            participantList = participantList,
                            currentParticipantsOfMeal = listOf(),
                            dateRange = generateDateRange(eventFromRepo.from, eventFromRepo.to),
                            selectedMeal = allMealsOfEvent.firstOrNull()
                                ?: Meal(day = Clock.System.now())
                        )
                    )
            } catch (e: Exception) {
                _eventState.value = ResultState.Error("Fehler beim Laden der Daten: ${e.message}")
            }
        }
    }


}
