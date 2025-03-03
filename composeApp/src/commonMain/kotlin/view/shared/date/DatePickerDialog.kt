package view.shared.date

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
 onSelect: ((selectedMilis: Long) -> Unit)
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds(),
        yearRange = IntRange(1950, 2025),
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = PastOrPresentSelectableDates
    )

    DatePickerDialog(
    shape = RoundedCornerShape(6.dp),
    onDismissRequest = {},
    confirmButton = {
        TextButton(onClick = {
            //Call function only if correct dates are selected
            if (datePickerState.selectedDateMillis != null) {
                onSelect(
                    datePickerState.selectedDateMillis!!,
                )
            }

            //selectedDate = datePickerState.selectedDateMillis!!
        }) {
            Text(text = "Bestätigen")
        }
    },
    ) {

        DatePicker(
            modifier = Modifier.weight(1f), // Important to display the button
            state = datePickerState,
        )

    }
}