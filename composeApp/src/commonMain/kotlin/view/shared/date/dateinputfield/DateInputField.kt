package view.shared.date.dateinputfield

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
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

@Composable
fun DateInputField(
    date: Instant?,
    interactionSource: MutableInteractionSource,
    label: String,
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
        onValueChange = {},
        interactionSource = interactionSource,
        modifier = Modifier
            .padding(8.dp).height(IntrinsicSize.Min),
        visualTransformation = DateTransformation()
    )
}