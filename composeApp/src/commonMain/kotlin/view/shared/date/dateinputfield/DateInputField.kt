package view.shared.date.dateinputfield

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import view.shared.HelperFunctions
import view.shared.date.FutureOrPresentSelectableDates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    date: Instant?,
    onDateChange: (Instant) -> Unit,
    label: String,
    selectableDates: SelectableDates = FutureOrPresentSelectableDates,
    selecteableDateError: String = "Datum liegt nicht in der Zukunft",
    trailingIcon: @Composable () -> Unit = {},
    isInputFieldEditable: Boolean = true,
) {
    var text by remember {
        mutableStateOf(
            "" + date?.let { date ->
                HelperFunctions.dateToNumberString(HelperFunctions.getLocalDate(date))
            },
        )
    }
    var error by remember { mutableStateOf("") }

    // Reset text when date changes externally
    LaunchedEffect(date) {
        text =
            date?.let { HelperFunctions.dateToNumberString(HelperFunctions.getLocalDate(it)) } ?: ""
    }

    OutlinedTextField(
        label = { Text(label) },
        isError = error.isNotEmpty(),
        supportingText = { Text(error, color = MaterialTheme.colorScheme.error) },
        singleLine = true,
        readOnly = !isInputFieldEditable,
        value = text,
        trailingIcon = trailingIcon,
        onValueChange = { newDate ->
            if (newDate.length <= 8 && newDate.all { it.isDigit() }) {
                text = newDate
                if (newDate.length == 8) {
                    try {
                        val parseDate = HelperFunctions.parseDate(newDate)
                        if (selectableDates.isSelectableDate(parseDate.toEpochMilliseconds())) {
                            onDateChange(parseDate)
                            error = ""
                        } else {
                            error = selecteableDateError
                        }
                    } catch (e: Exception) {
                        error = "Invalides Datum"
                    }
                }
            }
        },
        modifier = Modifier
            .padding(8.dp).height(IntrinsicSize.Min),
        visualTransformation = DateTransformation()
    )
}