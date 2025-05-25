package view.event.new_event

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import view.shared.HelperFunctions
import view.shared.date.DateRangePickerDialog
import view.shared.date.dateinputfield.DateInputField

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun SimpleDateRangePickerInDatePickerDialog(
    onSelect: (selectedStartMilis: Long, selectedEndMilis: Long) -> Unit,
    from: Instant?,
    to: Instant?,
    isEditable: Boolean = true,
    buttonText: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }

    var interactionSource = remember { MutableInteractionSource() }

    if (interactionSource.collectIsPressedAsState().value && isEditable) {
        showDatePicker = true
    }

    FlowRow(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        DateInputField(
            date = from,
            label = "Start:",
            isInputFieldEditable = isEditable,
            interactionSource = interactionSource
        )
        DateInputField(
            date = to,
            label = "Ende:",
            isInputFieldEditable = isEditable,
            interactionSource = interactionSource
        )
        Button(
            // Calendar icon to open DatePicker
            onClick = {
                if (isEditable) {
                    showDatePicker = true
                }
            },
            modifier = Modifier
                .padding(8.dp).height(IntrinsicSize.Min).align(Alignment.CenterVertically)
                .clip(shape = RoundedCornerShape(75))
                .background(
                    MaterialTheme.colorScheme.primary
                ),

            ) {
            Row {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                if (buttonText != null) {
                    Text(
                        text = buttonText,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
    if (showDatePicker) {
        DateRangePickerDialog(
            onSelect = { startMillis, endMillis ->
                onSelect(startMillis, endMillis)
                showDatePicker = false // Set value to false after onSelect is executed
            },
            startMillis = HelperFunctions.getMillisInUTC(from),
            endMillis = HelperFunctions.getMillisInUTC(to),
            onDismiss = { showDatePicker = false }
        )
    }
}
