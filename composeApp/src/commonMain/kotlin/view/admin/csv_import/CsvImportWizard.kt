package view.admin.csv_import

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.csv.*

enum class ImportStep {
    FILE_SELECTION,
    PREVIEW_AND_MAPPING,
    VALIDATION,
    IMPORT_PROGRESS,
    RESULTS
}

data class ImportWizardState(
    val currentStep: ImportStep = ImportStep.FILE_SELECTION,
    val selectedFile: FilePickerResult? = null,
    val csvData: CsvData? = null,
    val parseError: String? = null,
    val firstNameColumn: Int? = null,
    val lastNameColumn: Int? = null,
    val birthDateColumn: Int? = null,
    val validationResult: ValidationResult? = null,
    val importProgress: Float = 0f,
    val importComplete: Boolean = false,
    val importError: String? = null,
    val importedCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportWizard(
    state: ImportWizardState,
    onFileSelected: (FilePickerResult) -> Unit,
    onColumnMappingChanged: (Int?, Int?, Int?) -> Unit,
    onStartValidation: () -> Unit,
    onStartImport: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CSV Import") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Schließen")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ImportProgressIndicator(
                currentStep = state.currentStep,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            when (state.currentStep) {
                ImportStep.FILE_SELECTION -> {
                    FileSelectionStep(
                        selectedFile = state.selectedFile,
                        parseError = state.parseError,
                        onFileSelected = onFileSelected
                    )
                }

                ImportStep.PREVIEW_AND_MAPPING -> {
                    PreviewAndMappingStep(
                        csvData = state.csvData!!,
                        firstNameColumn = state.firstNameColumn,
                        lastNameColumn = state.lastNameColumn,
                        birthDateColumn = state.birthDateColumn,
                        onColumnMappingChanged = onColumnMappingChanged,
                        onNext = onStartValidation,
                        onBack = onReset
                    )
                }

                ImportStep.VALIDATION -> {
                    ValidationStep(
                        validationResult = state.validationResult!!,
                        onStartImport = onStartImport,
                        onBack = { /* Go back to mapping */ }
                    )
                }

                ImportStep.IMPORT_PROGRESS -> {
                    ImportProgressStep(
                        progress = state.importProgress,
                        importedCount = state.importedCount
                    )
                }

                ImportStep.RESULTS -> {
                    ResultsStep(
                        validationResult = state.validationResult!!,
                        importedCount = state.importedCount,
                        importError = state.importError,
                        onReset = onReset,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportProgressIndicator(
    currentStep: ImportStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "Datei auswählen",
        "Vorschau & Zuordnung",
        "Validierung",
        "Import",
        "Ergebnisse"
    )

    Column(modifier = modifier) {
        Text(
            text = "Import-Fortschritt",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, step ->
                val isActive = ImportStep.values()[index] == currentStep
                val isCompleted = ImportStep.values()[index].ordinal < currentStep.ordinal

                StepIndicator(
                    stepNumber = index + 1,
                    stepName = step,
                    isActive = isActive,
                    isCompleted = isCompleted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(
    stepNumber: Int,
    stepName: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.onPrimary
                        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Text(
            text = stepName,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun FileSelectionStep(
    selectedFile: FilePickerResult?,
    parseError: String?,
    onFileSelected: (FilePickerResult) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CSV-Datei auswählen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Wählen Sie eine CSV-Datei mit Teilnehmerdaten aus",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Unterstützte Formate: .csv, .txt\nErwartete Spalten: Vorname, Nachname, Geburtsdatum (optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Platform-specific file picker
                PlatformFilePicker(
                    onFileSelected = onFileSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                selectedFile?.let { file ->
                    Spacer(modifier = Modifier.height(16.dp))
                    if (file.error != null) {
                        Text(
                            text = "Fehler: ${file.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Datei ausgewählt: ${file.fileName}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                parseError?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fehler beim Lesen der Datei: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewAndMappingStep(
    csvData: CsvData,
    firstNameColumn: Int?,
    lastNameColumn: Int?,
    birthDateColumn: Int?,
    onColumnMappingChanged: (Int?, Int?, Int?) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
    ) {
        Text(
            text = "Vorschau und Spalten zuordnen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        CsvPreviewTable(
            csvData = csvData,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ColumnMappingCard(
            csvData = csvData,
            firstNameColumn = firstNameColumn,
            lastNameColumn = lastNameColumn,
            birthDateColumn = birthDateColumn,
            onFirstNameColumnChange = { firstName ->
                onColumnMappingChanged(firstName, lastNameColumn, birthDateColumn)
            },
            onLastNameColumnChange = { lastName ->
                onColumnMappingChanged(firstNameColumn, lastName, birthDateColumn)
            },
            onBirthDateColumnChange = { birthDate ->
                onColumnMappingChanged(firstNameColumn, lastNameColumn, birthDate)
            },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Zurück")
            }

            Button(
                onClick = onNext,
                enabled = firstNameColumn != null && lastNameColumn != null
            ) {
                Text("Weiter")
            }
        }
    }
}

@Composable
private fun ValidationStep(
    validationResult: ValidationResult,
    onStartImport: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Validierung",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ImportSummaryCard(
            totalRows = validationResult.validParticipants.size + validationResult.errors.size + validationResult.duplicates.size,
            validRows = validationResult.validParticipants.size,
            errorRows = validationResult.errors.size,
            duplicateRows = validationResult.duplicates.size,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (validationResult.errors.isNotEmpty()) {
            ErrorListCard(
                errors = validationResult.errors,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (validationResult.duplicates.isNotEmpty()) {
            DuplicateListCard(
                duplicates = validationResult.duplicates,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Zurück")
            }

            Button(
                onClick = onStartImport,
                enabled = validationResult.validParticipants.isNotEmpty()
            ) {
                Text("Import starten (${validationResult.validParticipants.size} Teilnehmer)")
            }
        }
    }
}

@Composable
private fun ImportProgressStep(
    progress: Float,
    importedCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Import läuft...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "${(progress * 100).toInt()}% - $importedCount Teilnehmer importiert",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResultsStep(
    validationResult: ValidationResult,
    importedCount: Int,
    importError: String?,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Import abgeschlossen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (importError == null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (importError == null) {
                    Text(
                        text = "✓ Import erfolgreich",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$importedCount Teilnehmer wurden erfolgreich importiert",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = "✗ Import fehlgeschlagen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Fehler: $importError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onReset) {
                Text("Neuer Import")
            }

            Button(onClick = onClose) {
                Text("Schließen")
            }
        }
    }
}

@Composable
private fun ErrorListCard(
    errors: List<ValidationError>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Fehler (${errors.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            errors.take(5).forEach { error ->
                Text(
                    text = "Zeile ${error.rowIndex}: ${error.field} - ${error.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (errors.size > 5) {
                Text(
                    text = "... und ${errors.size - 5} weitere Fehler",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DuplicateListCard(
    duplicates: List<ParticipantImportData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Duplikate (${duplicates.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            duplicates.take(5).forEach { duplicate ->
                Text(
                    text = "Zeile ${duplicate.rowIndex}: ${duplicate.firstName} ${duplicate.lastName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (duplicates.size > 5) {
                Text(
                    text = "... und ${duplicates.size - 5} weitere Duplikate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}