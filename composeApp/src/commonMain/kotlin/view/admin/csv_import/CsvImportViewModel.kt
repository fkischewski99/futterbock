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
import view.shared.ResultState

class CsvImportViewModel(
    private val eventRepository: EventRepository,
    private val allParticipantsViewModel: AllParticipantsViewModel
) : ViewModel() {

    private val _state = MutableStateFlow<ResultState<ImportWizardState>>(
        ResultState.Success(ImportWizardState())
    )
    val state: StateFlow<ResultState<ImportWizardState>> = _state.asStateFlow()

    private val csvParser = CsvParser()
    private val validator = ParticipantCsvValidator()

    fun onAction(action: CsvImportActions) {
        when (action) {
            is CsvImportActions.FileSelected -> handleFileSelected(action.result)
            is CsvImportActions.SetColumnMapping -> setColumnMapping(
                action.firstNameColumn,
                action.lastNameColumn,
                action.birthDateColumn
            )

            is CsvImportActions.StartValidation -> startValidation()
            is CsvImportActions.StartImport -> startImport()
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
        birthDateColumn: Int?
    ) {
        updateState { currentState ->
            currentState.copy(
                firstNameColumn = firstNameColumn,
                lastNameColumn = lastNameColumn,
                birthDateColumn = birthDateColumn
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
                    birthDateColumn = currentState.birthDateColumn
                )

                // Check for existing participants to detect duplicates in database
                val existingParticipants = try {
                    eventRepository.getAllParticipantsOfStamm()
                    // This would need to be collected, but for now we'll skip database duplicate checking
                    emptyList<Participant>()
                } catch (e: Exception) {
                    Logger.w("Could not fetch existing participants for duplicate check")
                    emptyList<Participant>()
                }

                updateState { currentState ->
                    currentState.copy(
                        validationResult = validationResult,
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
                importedCount = 0
            )
        }

        viewModelScope.launch {
            try {
                val validParticipants = validationResult.validParticipants
                var importedCount = 0

                validParticipants.forEachIndexed { index, participantData ->
                    try {
                        val participant = Participant().apply {
                            firstName = participantData.firstName
                            lastName = participantData.lastName
                            birthdate = participantData.birthDate
                        }

                        eventRepository.createNewParticipant(participant)
                        importedCount++
                        allParticipantsViewModel.addParticipant(participant)

                        // Update progress
                        val progress = (index + 1).toFloat() / validParticipants.size
                        updateState { currentState ->
                            currentState.copy(
                                importProgress = progress,
                                importedCount = importedCount
                            )
                        }

                        // Small delay to show progress (remove in production)
                        delay(100)

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

    private fun reset() {
        _state.value = ResultState.Success(ImportWizardState())
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