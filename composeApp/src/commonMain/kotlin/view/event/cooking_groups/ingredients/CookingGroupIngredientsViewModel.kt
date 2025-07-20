package view.event.cooking_groups.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import services.cookingGroups.CookingGroupIngredientService
import services.shoppingList.CalculateShoppingList
import view.event.SharedEventViewModel
import view.event.new_event.groupMealsByDate
import view.shared.ResultState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import model.Participant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for managing cooking group ingredient distribution data.
 * Provides day-based navigation and ingredient calculations per cooking group.
 * Uses SharedEventViewModel to get event data and meal information.
 */
class CookingGroupIngredientsViewModel(
    private val calculateShoppingList: CalculateShoppingList
) : ViewModel(), KoinComponent {


    private val sharedEventViewModel: SharedEventViewModel by inject()
    private val cookingGroupService = CookingGroupIngredientService(calculateShoppingList)

    private val _state = MutableStateFlow<ResultState<CookingGroupIngredientsState>>(
        ResultState.Loading
    )
    val state: StateFlow<ResultState<CookingGroupIngredientsState>> = _state.asStateFlow()

    private var mealsByDate: Map<LocalDate, List<model.Meal>> = emptyMap()
    private var eventDates: List<LocalDate> = emptyList()

    /**
     * Initializes the ViewModel and loads the initial data.
     * Uses SharedEventViewModel to get event and meal data.
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                _state.value = ResultState.Loading

                // Get event from SharedEventViewModel
                val eventState = sharedEventViewModel.eventState.value
                val event = eventState.getSuccessData()?.event

                if (event == null) {
                    _state.value =
                        ResultState.Error(Exception("Event nicht vorhanden").toString())
                    return@launch
                }

                // Get meals from SharedEventViewModel
                val allMeals = eventState.getSuccessData()?.mealList ?: emptyList()

                // Use existing groupMealsByDate function to get meals organized by date
                mealsByDate = groupMealsByDate(event.from, event.to, allMeals)
                eventDates = mealsByDate.keys.sorted()

                Logger.d("Initialized with ${eventDates.size} event dates and ${allMeals.size} total meals")

                // Determine initial date (today if in range, otherwise first event day)
                val initialDate = determineInitialDate(eventDates)

                // Load data for initial date
                loadDataForDate(event.uid, initialDate)

            } catch (e: Exception) {
                Logger.e("Error initializing cooking group ingredients", e)
                _state.value = ResultState.Error("Fehler beim Laden der Kochgruppen")
            }
        }
    }

    /**
     * Navigates to the previous day.
     */
    private fun goToPreviousDay() {
        val currentState = getCurrentState() ?: return
        val currentIndex = eventDates.indexOf(currentState.currentDate)

        if (currentIndex > 0) {
            val previousDate = eventDates[currentIndex - 1]
            navigateToDate(previousDate)
        }
    }

    /**
     * Navigates to the next day.
     */
    private fun goToNextDay() {
        val currentState = getCurrentState() ?: return
        val currentIndex = eventDates.indexOf(currentState.currentDate)

        if (currentIndex < eventDates.size - 1) {
            val nextDate = eventDates[currentIndex + 1]
            navigateToDate(nextDate)
        }
    }

    /**
     * Navigates to a specific date.
     */
    private fun navigateToDate(date: LocalDate) {
        if (!eventDates.contains(date)) {
            Logger.w("Attempted to navigate to date $date which is not in event range")
            return
        }

        // Get event ID from SharedEventViewModel
        val eventId = sharedEventViewModel.eventState.value.getSuccessData()?.event?.uid
        if (eventId == null) {
            Logger.e("Event ID not available from SharedEventViewModel")
            return
        }

        viewModelScope.launch {
            loadDataForDate(eventId, date)
        }
    }

    /**
     * Refreshes the data for the current date.
     */
    private fun refresh() {
        val currentState = getCurrentState() ?: return

        // Get event ID from SharedEventViewModel
        val eventId = sharedEventViewModel.eventState.value.getSuccessData()?.event?.uid
        if (eventId == null) {
            Logger.e("Event ID not available from SharedEventViewModel")
            return
        }

        viewModelScope.launch {
            loadDataForDate(eventId, currentState.currentDate)
        }
    }

    /**
     * Loads cooking group ingredients data for a specific date.
     */
    private suspend fun loadDataForDate(eventId: String, date: LocalDate) {
        try {
            Logger.d("Loading cooking group ingredients for $date")

            // Update state to show loading
            updateState { currentState ->
                currentState.copy(
                    currentDate = date,
                    isLoading = true,
                    error = null
                )
            }

            // Get meals for the specific date from the pre-grouped meals
            val mealsForDate = mealsByDate[date] ?: emptyList()

            Logger.d("Found ${mealsForDate.size} meals for $date")

            // Get participants from SharedEventViewModel
            val eventState = sharedEventViewModel.eventState.value.getSuccessData()
            val participants = eventState?.participantList ?: emptyList()

            Logger.d("Loaded ${participants.size} participants from SharedEventViewModel")

            // Calculate cooking group ingredients
            val cookingGroupIngredients = cookingGroupService.getCookingGroupIngredientsForDay(
                eventId = eventId,
                date = date,
                meals = mealsForDate,
                participants = participants.filter { it.participant != null }

            )
            // Filter out ingredients that are not needed
            val ingredientsToFilter = listOf("salz", "pfeffer", "wasser")
            cookingGroupIngredients.forEach { cookingGroupIngredients ->
                cookingGroupIngredients.ingredients =
                    cookingGroupIngredients.ingredients.filter { ingredient ->
                        !ingredientsToFilter.contains(
                            ingredient.ingredient?.name?.lowercase() ?: ""
                        )
                    }
            }

            // Update state with results
            updateState { currentState ->
                currentState.copy(
                    cookingGroupIngredients = cookingGroupIngredients,
                    isLoading = false,
                    error = null,
                    hasPreviousDay = eventDates.indexOf(date) > 0,
                    hasNextDay = eventDates.indexOf(date) < eventDates.size - 1
                )
            }

            Logger.d("Successfully loaded ingredients for ${cookingGroupIngredients.size} cooking groups")

        } catch (e: Exception) {
            Logger.e("Error loading cooking group ingredients for $date", e)
            updateState { currentState ->
                currentState.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Determines the initial date to display (today if in event range, otherwise first event day).
     */
    private fun determineInitialDate(eventDates: List<LocalDate>): LocalDate {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        return if (eventDates.contains(today)) today else eventDates.firstOrNull() ?: today
    }

    /**
     * Gets the current state if it's a success state.
     */
    private fun getCurrentState(): CookingGroupIngredientsState? {
        return when (val currentState = _state.value) {
            is ResultState.Success -> currentState.data
            else -> null
        }
    }

    /**
     * Updates the state using the provided update function.
     */
    private fun updateState(update: (CookingGroupIngredientsState) -> CookingGroupIngredientsState) {
        val currentState = getCurrentState()
        if (currentState != null) {
            _state.value = ResultState.Success(update(currentState))
        } else {
            // Initialize with default state if none exists
            _state.value = ResultState.Success(update(CookingGroupIngredientsState()))
        }
    }

    fun onAction(actions: CookingGroupIngredientActions) {
        when (actions) {
            is CookingGroupIngredientActions.PreviousDay -> goToPreviousDay()
            is CookingGroupIngredientActions.NextDay -> goToNextDay()
            is CookingGroupIngredientActions.Refresh -> refresh()
            CookingGroupIngredientActions.Initilize -> initialize()
        }
    }
}

/**
 * State data class for cooking group ingredients screen.
 */
data class CookingGroupIngredientsState(
    val currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date,
    val cookingGroupIngredients: List<CookingGroupIngredientService.CookingGroupIngredients> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPreviousDay: Boolean = false,
    val hasNextDay: Boolean = false
)