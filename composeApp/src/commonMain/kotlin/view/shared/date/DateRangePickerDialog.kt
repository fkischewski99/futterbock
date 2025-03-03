package view.shared.date

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onSelect: ((selectedStartMilis: Long, selectedEndMilis: Long) -> Unit),
    onDismiss: () -> Unit,
    startMillis: Long,
    endMillis: Long,
    selectableDates: SelectableDates = FutureOrPresentSelectableDates
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startMillis,
        initialSelectedEndDateMillis = endMillis,
        yearRange = IntRange(2023, 2100),
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = selectableDates
    )

    DatePickerDialog(
        shape = RoundedCornerShape(6.dp),
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                //Call function only if correct dates are selected
                if (dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null) {
                    onSelect(
                        dateRangePickerState.selectedStartDateMillis!!,
                        dateRangePickerState.selectedEndDateMillis!!
                    )
                }

                //selectedDate = datePickerState.selectedDateMillis!!
            }) {
                Text(text = "Bestätigen")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(text = "Abbrechen")
            }
        }
    ) {

        DateRangePicker(
            modifier = Modifier.weight(1f), // Important to display the button
            state = dateRangePickerState,
        )

    }
}