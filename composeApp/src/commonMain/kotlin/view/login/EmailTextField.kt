import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.text.input.ImeAction
import futterbock_app.composeapp.generated.resources.Res
import futterbock_app.composeapp.generated.resources.email
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import view.login.autofill

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
@Composable
fun EmailTextField(
    email: String,
    onEmailChange: (value: String) -> Unit,
    loading: Boolean,
    onNext: () -> Unit = {},
) {
    TextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(text = stringResource(Res.string.email)) },
        modifier =
        Modifier
            .fillMaxWidth()
            .autofill(
                autofillTypes = listOf(AutofillType.EmailAddress),
                onFill = onEmailChange,
            ),
        singleLine = true,
        keyboardOptions =
        KeyboardOptions(
            imeAction = ImeAction.Next,
        ),
        keyboardActions =
        KeyboardActions(
            onNext = { onNext() },
        ),
        enabled = !loading,
    )
}