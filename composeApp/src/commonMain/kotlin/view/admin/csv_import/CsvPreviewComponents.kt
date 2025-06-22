package view.admin.csv_import

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.csv.CsvData

@Composable
fun CsvPreviewTable(
    csvData: CsvData,
    maxPreviewRows: Int = 10,
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
                text = "CSV Vorschau (${csvData.rows.size} Zeilen)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), // Fixed height to avoid conflicts
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Header row
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        itemsIndexed(csvData.headers) { index, header ->
                            CsvCell(
                                text = header,
                                isHeader = true,
                            )
                        }
                    }
                }

                // Data rows
                items(csvData.rows.take(maxPreviewRows)) { row ->
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        itemsIndexed(row) { index, cell ->
                            CsvCell(
                                text = cell,
                                isHeader = false,
                            )
                        }
                    }
                }

                // Truncation message
                if (csvData.rows.size > maxPreviewRows) {
                    item {
                        Text(
                            text = "... und ${csvData.rows.size - maxPreviewRows} weitere Zeilen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CsvCell(
    text: String,
    isHeader: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cellColor =
        if (isHeader) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor =
        if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .width(160.dp)
            .height(IntrinsicSize.Min)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(cellColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            style = if (isHeader) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnMappingCard(
    csvData: CsvData,
    firstNameColumn: Int?,
    lastNameColumn: Int?,
    birthDateColumn: Int?,
    onFirstNameColumnChange: (Int?) -> Unit,
    onLastNameColumnChange: (Int?) -> Unit,
    onBirthDateColumnChange: (Int?) -> Unit,
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
                text = "Spalten zuordnen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // First Name Column
            ColumnSelector(
                label = "Vorname *",
                selectedColumn = firstNameColumn,
                options = csvData.headers,
                onSelectionChange = onFirstNameColumnChange,
                required = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Last Name Column  
            ColumnSelector(
                label = "Nachname *",
                selectedColumn = lastNameColumn,
                options = csvData.headers,
                onSelectionChange = onLastNameColumnChange,
                required = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Birth Date Column
            ColumnSelector(
                label = "Geburtsdatum (optional)",
                selectedColumn = birthDateColumn,
                options = csvData.headers,
                onSelectionChange = onBirthDateColumnChange,
                required = false,
                allowNone = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "* Pflichtfelder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnSelector(
    label: String,
    selectedColumn: Int?,
    options: List<String>,
    onSelectionChange: (Int?) -> Unit,
    required: Boolean,
    allowNone: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = when {
                    selectedColumn == null && allowNone -> "Keine Zuordnung"
                    selectedColumn != null && selectedColumn < options.size -> options[selectedColumn]
                    else -> ""
                },
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (required && selectedColumn == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // None option for optional fields
                if (allowNone) {
                    DropdownMenuItem(
                        text = { Text("Keine Zuordnung") },
                        onClick = {
                            onSelectionChange(null)
                            expanded = false
                        }
                    )
                }

                // Column options
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${index + 1}. $option",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            onSelectionChange(index)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (required && selectedColumn == null) {
            Text(
                text = "Dieses Feld ist erforderlich",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ImportSummaryCard(
    totalRows: Int,
    validRows: Int,
    errorRows: Int,
    duplicateRows: Int,
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
                text = "Import-Zusammenfassung",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Gesamt",
                    value = totalRows,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SummaryItem(
                    label = "Gültig",
                    value = validRows,
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryItem(
                    label = "Fehler",
                    value = errorRows,
                    color = MaterialTheme.colorScheme.error
                )
                SummaryItem(
                    label = "Duplikate",
                    value = duplicateRows,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}