package view.admin.csv_import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import data.EventRepository
import data.csv.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.Participant
import view.event.new_meal_screen.AllParticipantsViewModel
import view.event.participants.EditParticipantActions
import view.event.SharedEventViewModel
import view.shared.ResultState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CsvImportViewModel(
    private val eventRepository: EventRepository,
    private val allParticipantsViewModel: AllParticipantsViewModel
) : ViewModel(), KoinComponent {

    private val sharedEventViewModel: SharedEventViewModel by inject()

    private val _state = MutableStateFlow<ResultState<ImportWizardState>>(
        ResultState.Success(ImportWizardState())
    )
    val state: StateFlow<ResultState<ImportWizardState>> = _state.asStateFlow()

    private val csvParser = CsvParser()
    private val validator = ParticipantCsvValidator()
    private val eventImportService = EventParticipantImportService(eventRepository)

    fun onAction(action: CsvImportActions) {
        when (action) {
            is CsvImportActions.FileSelected -> handleFileSelected(action.result)
            is CsvImportActions.SetColumnMapping -> setColumnMapping(
                action.firstNameColumn,
                action.lastNameColumn,
                action.birthDateColumn,
                action.eatingHabitColumn
            )

            is CsvImportActions.StartValidation -> startValidation()
            is CsvImportActions.StartImport -> startImport()
            is CsvImportActions.CancelImport -> cancelImport()
            is CsvImportActions.GoBack -> goBack()
            is CsvImportActions.Reset -> reset()
            is CsvImportActions.Close -> {
                // Handle close action - would typically navigate back
            }
        }
    }


    private fun handleFileSelected(result: FilePickerResult) {
        updateState { currentState ->
            currentState.copy(
                selectedFile = result,
                parseError = null
            )
        }

        if (result.content != null) {
            parseCSV(result.content)
        }
    }

    private fun parseCSV(content: String) {
        viewModelScope.launch {
            val parseResult = csvParser.parseCSV(content)

            if (parseResult.data != null) {
                updateState { currentState ->
                    currentState.copy(
                        csvData = parseResult.data,
                        currentStep = ImportStep.PREVIEW_AND_MAPPING,
                        parseError = null,
                        // Auto-detect common column names
                        firstNameColumn = detectColumn(
                            parseResult.data.headers,
                            listOf("vorname", "first", "firstname", "first_name")
                        ),
                        lastNameColumn = detectColumn(
                            parseResult.data.headers,
                            listOf("nachname", "last", "lastname", "last_name", "surname")
                        ),
                        birthDateColumn = detectColumn(
                            parseResult.data.headers,
                            listOf("geburtsdatum", "birthdate", "birth_date", "geboren", "birthday")
                        ),
                        eatingHabitColumn = detectColumn(
                            parseResult.data.headers,
                            listOf(
                                "essgewohnheit",
                                "eating_habit",
                                "ernaehrung",
                                "diaet",
                                "diet",
                                "habit"
                            )
                        )
                    )
                }
            } else {
                updateState { currentState ->
                    currentState.copy(
                        parseError = parseResult.error
                    )
                }
            }
        }
    }

    private fun detectColumn(headers: List<String>, keywords: List<String>): Int? {
        return headers.indexOfFirst { header ->
            keywords.any { keyword ->
                header.lowercase().contains(keyword.lowercase())
            }
        }.takeIf { it >= 0 }
    }

    private fun setColumnMapping(
        firstNameColumn: Int?,
        lastNameColumn: Int?,
        birthDateColumn: Int?,
        eatingHabitColumn: Int?
    ) {
        updateState { currentState ->
            currentState.copy(
                firstNameColumn = firstNameColumn,
                lastNameColumn = lastNameColumn,
                birthDateColumn = birthDateColumn,
                eatingHabitColumn = eatingHabitColumn
            )
        }
    }

    private fun startValidation() {
        val currentState = getCurrentState() ?: return
        val csvData = currentState.csvData ?: return
        val firstNameColumn = currentState.firstNameColumn ?: return
        val lastNameColumn = currentState.lastNameColumn ?: return

        viewModelScope.launch {
            try {
                val validationResult = validator.validateParticipantData(
                    csvData = csvData,
                    firstNameColumn = firstNameColumn,
                    lastNameColumn = lastNameColumn,
                    birthDateColumn = currentState.birthDateColumn,
                    eatingHabitColumn = currentState.eatingHabitColumn
                )

                // Check for existing participants to detect duplicates in database
                val existingParticipants = try {
                    allParticipantsViewModel.state.value.getSuccessData()?.allParticipants
                        ?: emptyList()
                } catch (e: Exception) {
                    Logger.w("Could not fetch existing participants for duplicate check: ${e.message}")
                    emptyList()
                }

                // For event-specific imports, also check participants already in the event
                val existingEventParticipants = if (currentState.eventId != null) {
                    try {
                        eventRepository.getParticipantsOfEvent(
                            currentState.eventId,
                            withParticipant = true
                        )
                            .mapNotNull { participantTime ->
                                existingParticipants.find { it.uid == participantTime.participantRef }
                            }
                    } catch (e: Exception) {
                        Logger.w("Could not fetch existing event participants: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // For event imports, only filter out participants already in the event
                // For general imports, filter out participants already in the database
                val filteredValidParticipants = if (currentState.eventId != null) {
                    // Event import: only exclude participants already in this event
                    validationResult.validParticipants.filter { importData ->
                        val isEventDuplicate = existingEventParticipants.any { existing ->
                            existing.firstName.equals(importData.firstName, ignoreCase = true) &&
                                    existing.lastName.equals(importData.lastName, ignoreCase = true)
                        }
                        !isEventDuplicate
                    }
                } else {
                    // General import: exclude participants already in the database
                    validationResult.validParticipants.filter { importData ->
                        val isDatabaseDuplicate = existingParticipants.any { existing ->
                            existing.firstName.equals(importData.firstName, ignoreCase = true) &&
                                    existing.lastName.equals(importData.lastName, ignoreCase = true)
                        }
                        !isDatabaseDuplicate
                    }
                }

                // Create appropriate duplicate lists based on import type
                val databaseDuplicates = if (currentState.eventId == null) {
                    // Only show database duplicates for general imports
                    validationResult.validParticipants.filter { importData ->
                        existingParticipants.any { existing ->
                            existing.firstName.equals(importData.firstName, ignoreCase = true) &&
                                    existing.lastName.equals(importData.lastName, ignoreCase = true)
                        }
                    }
                } else {
                    emptyList()
                }


                // Update validation result with filtered participants
                val finalValidationResult = validationResult.copy(
                    validParticipants = filteredValidParticipants,
                    duplicates = validationResult.duplicates + databaseDuplicates
                )

                updateState { currentState ->
                    currentState.copy(
                        validationResult = finalValidationResult,
                        currentStep = ImportStep.VALIDATION
                    )
                }
            } catch (e: Exception) {
                Logger.e("Validation error", e)
                updateState { currentState ->
                    currentState.copy(
                        parseError = "Validierungsfehler: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startImport() {
        val currentState = getCurrentState() ?: return
        val validationResult = currentState.validationResult ?: return

        if (validationResult.validParticipants.isEmpty()) {
            Logger.w("No valid participants to import")
            return
        }

        updateState { currentState ->
            currentState.copy(
                currentStep = ImportStep.IMPORT_PROGRESS,
                importProgress = 0f,
                importedCount = 0,
                participantsAddedToEvent = 0,
                participantsCreated = 0,
                participantsFound = 0
            )
        }

        viewModelScope.launch {
            try {
                val validParticipants = validationResult.validParticipants

                if (currentState.eventId != null) {
                    // Event-specific import
                    handleEventImport(currentState.eventId, validParticipants)
                } else {
                    // General participant import
                    handleGeneralImport(validParticipants)
                }

            } catch (e: Exception) {
                Logger.e("Import failed", e)
                updateState { currentState ->
                    currentState.copy(
                        currentStep = ImportStep.RESULTS,
                        importError = e.message ?: "Unbekannter Fehler beim Import"
                    )
                }
            }
        }
    }

    private suspend fun handleEventImport(
        eventId: String,
        participants: List<ParticipantImportData>
    ) {
        try {
            val result = eventImportService.importParticipantsToEvent(
                eventId = eventId,
                participants = participants,
                addParticipantCallback = { participant ->
                    sharedEventViewModel.onAction(EditParticipantActions.AddParticipant(participant))
                },
                progressCallback = { processed, total ->
                    val progress = processed.toFloat() / total
                    updateState { currentState ->
                        currentState.copy(
                            importProgress = progress,
                            importedCount = processed
                        )
                    }
                }
            )

            updateState { currentState ->
                currentState.copy(
                    currentStep = ImportStep.RESULTS,
                    importComplete = true,
                    participantsAddedToEvent = result.participantsAddedToEvent,
                    participantsCreated = result.participantsCreated,
                    participantsFound = result.participantsFound,
                    importedCount = result.participantsAddedToEvent,
                    importError = if (result.errors.isNotEmpty()) {
                        "Einige Teilnehmer konnten nicht importiert werden: ${result.errors.size} Fehler"
                    } else null
                )
            }

        } catch (e: Exception) {
            Logger.e("Event import failed", e)
            updateState { currentState ->
                currentState.copy(
                    currentStep = ImportStep.RESULTS,
                    importError = "Event-Import fehlgeschlagen: ${e.message}"
                )
            }
        }
    }

    private suspend fun handleGeneralImport(participants: List<ParticipantImportData>) {
        var importedCount = 0

        participants.forEachIndexed { index, participantData ->
            try {
                val participant = Participant().apply {
                    firstName = participantData.firstName
                    lastName = participantData.lastName
                    birthdate = participantData.birthDate
                    eatingHabit = participantData.eatingHabit
                }

                val participantCreated = eventRepository.createNewParticipant(participant)
                if (participantCreated != null) {
                    importedCount++
                    allParticipantsViewModel.addParticipant(participant)
                }

                // Update progress
                val progress = (index + 1).toFloat() / participants.size
                updateState { currentState ->
                    currentState.copy(
                        importProgress = progress,
                        importedCount = importedCount
                    )
                }

            } catch (e: Exception) {
                Logger.e(
                    "Error importing participant: ${participantData.firstName} ${participantData.lastName}",
                    e
                )
                // Continue with other participants
            }
        }

        // Import completed
        updateState { currentState ->
            currentState.copy(
                currentStep = ImportStep.RESULTS,
                importComplete = true,
                importedCount = importedCount
            )
        }
    }

    private fun goBack() {
        updateState { currentState ->
            when (currentState.currentStep) {
                ImportStep.PREVIEW_AND_MAPPING -> {
                    currentState.copy(
                        currentStep = ImportStep.FILE_SELECTION,
                        csvData = null,
                        firstNameColumn = null,
                        lastNameColumn = null,
                        birthDateColumn = null,
                        eatingHabitColumn = null,
                        parseError = null
                    )
                }

                ImportStep.VALIDATION -> {
                    currentState.copy(
                        currentStep = ImportStep.PREVIEW_AND_MAPPING,
                        validationResult = null
                    )
                }

                ImportStep.RESULTS -> {
                    // Don't allow going back from results, use reset instead
                    currentState
                }

                else -> currentState
            }
        }
    }

    private fun cancelImport() {
        updateState { currentState ->
            currentState.copy(
                currentStep = ImportStep.RESULTS,
                importComplete = false,
                importError = "Import wurde abgebrochen",
                importProgress = 0f
            )
        }
    }

    fun setEventId(eventId: String?) {
        updateState { currentState ->
            currentState.copy(eventId = eventId)
        }
    }


    private fun reset() {
        val currentEventId = getCurrentState()?.eventId
        _state.value = ResultState.Success(ImportWizardState(eventId = currentEventId))
    }

    private fun getCurrentState(): ImportWizardState? {
        return when (val currentState = _state.value) {
            is ResultState.Success -> currentState.data
            else -> null
        }
    }

    private fun updateState(update: (ImportWizardState) -> ImportWizardState) {
        val currentState = getCurrentState()
        if (currentState != null) {
            _state.value = ResultState.Success(update(currentState))
        }
    }
}