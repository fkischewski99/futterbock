package view.shared.step_counter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StepperCounter(
    initialValue: Int = 0,
    onValueChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    bottomContent: (@Composable () -> Unit)? = null
) {
    var count by remember { mutableStateOf(initialValue) }
    var inputText by remember { mutableStateOf(count.toString()) }

    fun updateCount(newCount: Int) {
        count = newCount
        inputText = newCount.toString()
        onValueChange(newCount)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircleIconButton(
                icon = Icons.Default.Remove,
                contentDescription = "Decrease",
                onClick = { updateCount(count - 1) }
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = { newText ->
                    if (newText.matches(Regex("^-?\\d*\$"))) {
                        inputText = newText
                        newText.toIntOrNull()?.let {
                            count = it
                            onValueChange(it)
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(90.dp)
                    .height(56.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            )

            CircleIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Increase",
                onClick = { updateCount(count + 1) }
            )
        }

        if (bottomContent != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                bottomContent()
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        color = Color.Transparent,
        modifier = Modifier.size(40.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
