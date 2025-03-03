package view.event.new_meal_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.EatingHabit
import model.FoodIntolerance
import model.Range
import model.RecipeType
import model.Season
import model.TimeRange

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    InputChip(
        label = {
            Box(contentAlignment = Alignment.CenterStart) {
                Text(text = text)
            }
        },
        onClick = onClick,
        selected = isSelected,
        modifier = Modifier.padding(start = 4.dp),
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done icon",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun <T : Enum<T>> SelectionRow(
    filterOption: T,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDeselect: (() -> Unit)? = null
) {
    var checkedState by remember { mutableStateOf(isSelected) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checkedState = !checkedState
                if (isSelected) {
                    onDeselect?.invoke()
                } else {
                    onSelect()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Conditional rendering of Checkbox or RadioButton based on the selection mode
        when (onDeselect) {
            null -> {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
            }

            else -> {
                Checkbox(
                    checked = checkedState,
                    onCheckedChange = { checked ->
                        checkedState = checked
                        if (checked) {
                            onSelect() // When checked
                        } else {
                            onDeselect() // When unchecked
                        }
                    }
                )
            }
        }
        Text(text = filterOption.toString())
    }
}

@Composable
fun <T : Enum<T>> SingleSelectFilterDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedFilter: T?,
    onFilterSelect: (T) -> Unit,
    deleteFilter: () -> Unit,
    filterOptions: List<T>
) {
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Wähle einen Filter") },
            text = {
                Column {
                    filterOptions.forEach { filterOption ->
                        SelectionRow(
                            filterOption = filterOption,
                            isSelected = (filterOption == selectedFilter),
                            onSelect = { onFilterSelect(filterOption) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = deleteFilter) {
                    Text("Filter löschen")
                }
            },
        )
    }
}

@Composable
fun <T : Enum<T>> MultiSelectFilterDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedFilters: Set<T>,
    onFilterSelect: (T) -> Unit,
    onFilterDeselect: (T) -> Unit,
    deleteFilter: () -> Unit,
    filterOptions: List<T>
) {
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Wähle einen oder mehrere Filter") },
            text = {
                Column {
                    filterOptions.forEach { filterOption ->
                        SelectionRow(
                            filterOption = filterOption,
                            isSelected = selectedFilters.contains(filterOption),
                            onSelect = { onFilterSelect(filterOption) },
                            onDeselect = { onFilterDeselect(filterOption) }
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = deleteFilter) {
                    Text("Filter löschen")
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Filter übernehmen")
                }
            },
        )
    }
}

@Composable
fun EatingHabitFilter(
    onFilterSelect: (EatingHabit?) -> Unit,
    selectedEatingHabitFilter: EatingHabit?,
) {
    SingleFilterForEnum(
        filterName = "Essgewohnheit",
        listOfValues = EatingHabit.entries,
        onFilterSelect = onFilterSelect,
        selectedFilter = selectedEatingHabitFilter
    )
}

@Composable
fun FoodIntoleranceFilter(
    selectedIntolerances: Set<FoodIntolerance>,
    onFiltersChange: (Set<FoodIntolerance>) -> Unit
) {
    MultiSelectFilterForEnum(
        filterName = "Intoleranz",
        filterNamePlural = "Intoleranzen",
        listOfValues = FoodIntolerance.entries,
        selectedIntolerances = selectedIntolerances,
        onFiltersChange = onFiltersChange
    )
}

@Composable
fun <T : Enum<T>> MultiSelectFilterForEnum(
    filterName: String,
    filterNamePlural: String,
    listOfValues: List<T>,
    selectedIntolerances: Set<T>,
    onFiltersChange: (Set<T>) -> Unit
) {
    var openFilterDialog by remember { mutableStateOf(false) }
    var isFilterActive by remember(openFilterDialog) {
        mutableStateOf(selectedIntolerances.isNotEmpty())
    }

    // Create a display text for selected intolerances
    val displayText = if (isFilterActive) {
        "$filterName: ${selectedIntolerances.joinToString(", ")}"
    } else {
        filterNamePlural
    }

    FilterChip(
        text = displayText,
        isSelected = isFilterActive,
        onClick = { openFilterDialog = true }
    )

    MultiSelectFilterDialog(
        isOpen = openFilterDialog,
        onDismiss = {
            openFilterDialog = false
        },
        selectedFilters = selectedIntolerances,
        onFilterSelect = {
            onFiltersChange(selectedIntolerances + it)
        },
        onFilterDeselect = {
            onFiltersChange(selectedIntolerances - it)
        },
        deleteFilter = {
            openFilterDialog = false
            onFiltersChange(emptySet())
        },
        filterOptions = listOfValues
    )
}

@Composable
fun <T : Enum<T>> SingleFilterForEnum(
    filterName: String,
    listOfValues: List<T>,
    onFilterSelect: (T?) -> Unit,
    selectedFilter: T?,
) {
    var openFilterDialog by remember { mutableStateOf(false) }
    FilterChip(
        text = filterName + if (selectedFilter != null) ": $selectedFilter" else "",
        isSelected = selectedFilter != null,
        onClick = { openFilterDialog = true }
    )
    SingleSelectFilterDialog(
        isOpen = openFilterDialog,
        onDismiss = { openFilterDialog = false },
        selectedFilter = selectedFilter,
        onFilterSelect = {
            openFilterDialog = false
            onFilterSelect(it)
        },
        deleteFilter = {
            openFilterDialog = false
            onFilterSelect(null)
        },
        filterOptions = listOfValues,
    )
}

@Composable
fun PriceFilter(
    onFilterSelect: (Range?) -> Unit,
    selectedPriceFilter: Range?,
) {
    SingleFilterForEnum(
        filterName = "Preis",
        listOfValues = Range.entries,
        onFilterSelect = onFilterSelect,
        selectedFilter = selectedPriceFilter
    )
}

@Composable
fun RecipeTypeFilter(
    onFilterSelect: (RecipeType?) -> Unit,
    selectedRecipeType: RecipeType?,
) {
    SingleFilterForEnum(
        filterName = "Typ",
        listOfValues = RecipeType.entries,
        onFilterSelect = onFilterSelect,
        selectedFilter = selectedRecipeType
    )
}

@Composable
fun SeasonFilter(
    onFilterSelect: (Season?) -> Unit,
    selectedRecipeType: Season?,
) {
    SingleFilterForEnum(
        filterName = "Saison",
        listOfValues = Season.entries,
        onFilterSelect = onFilterSelect,
        selectedFilter = selectedRecipeType
    )
}

@Composable
fun SkillLevelFilter(
    onFilterSelect: (Range?) -> Unit,
    selectedRecipeType: Range?,
) {
    SingleFilterForEnum(
        filterName = "Skill Level",
        listOfValues = Range.entries,
        onFilterSelect = onFilterSelect,
        selectedFilter = selectedRecipeType
    )
}

@Composable
fun TimeFilter(
    onFilterSelect: (TimeRange?) -> Unit,
    selectedTimeFilter: TimeRange?,
) {
    SingleFilterForEnum(
        filterName = "Dauer",
        listOfValues = TimeRange.entries,
        onFilterSelect = onFilterSelect,
        selectedFilter = selectedTimeFilter
    )
}