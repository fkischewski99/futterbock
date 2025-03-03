package view.login

import EmailTextField
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.andreasgift.kmpweatherapp.BuildKonfig
import futterbock_app.composeapp.generated.resources.Res
import futterbock_app.composeapp.generated.resources.login_submit
import futterbock_app.composeapp.generated.resources.register_link
import futterbock_app.composeapp.generated.resources.register_prefix
import futterbock_app.composeapp.generated.resources.startPage
import getPlatformName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import services.login.LoginAndRegister
import view.shared.MGCircularProgressIndicator


@Composable
fun LoginScreen(
    navigateToRegister: () -> Unit,
    navigateToHome: () -> Unit,
) {
    var loading by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loginError by rememberSaveable { mutableStateOf("") }
    val isSubmitEnabled = remember(email, password, loading) {
        !loading && (email.isNotBlank() && password.isNotBlank())
    }
    val scope = rememberCoroutineScope()
    val loginService: LoginAndRegister = koinInject<LoginAndRegister>()

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
    ) {
        LoginContent(
            onPasswordChange = { value: String -> password = value },
            onEmailChange = { value: String -> email = value },
            email = email,
            password = password,
            loading = loading,
            isSubmitEnabled = isSubmitEnabled,
            loginError = loginError,
            navigateToRegister = navigateToRegister,
            onSubmit = {
                scope.launch {
                    loading = true
                    try {
                        loginService.login(email = email, password = password)
                        navigateToHome()
                    } catch (t: Throwable) {
                        loginError =
                            "Fehler beim Anmelden. Bitte überprüfe deine Email und dein Passwort"
                    } finally {
                        loading = false
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LoginContent(
    onEmailChange: (value: String) -> Unit,
    onPasswordChange: (value: String) -> Unit,
    onSubmit: () -> Unit,
    email: String,
    password: String,
    loading: Boolean,
    isSubmitEnabled: Boolean,
    loginError: String,
    navigateToRegister: () -> Unit,
) {
    val focusManager by rememberUpdatedState(LocalFocusManager.current)

    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp)
            .clickable { focusManager.clearFocus() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.startPage),
            contentDescription = "logo",
            Modifier.height(300.dp)
            //colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primary),
        )

        Spacer(modifier = Modifier.padding(16.dp))
        EmailTextField(
            email = email,
            onEmailChange = onEmailChange,
            loading = loading,
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            },
        )
        Spacer(modifier = Modifier.padding(4.dp))
        PasswordTextField(
            password = password,
            onPasswordChange = onPasswordChange,
            loading = loading,
            onDone = { focusManager.clearFocus() },
        )
        Spacer(modifier = Modifier.padding(8.dp))
        ErrorField(loginError)
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isSubmitEnabled,
        ) {
            if (!loading) {
                Text(text = stringResource(resource = Res.string.login_submit))
            } else {
                MGCircularProgressIndicator()
            }
        }
        Spacer(modifier = Modifier.padding(16.dp))
        RegisterLink(onNavigateToRegister = navigateToRegister)

    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this.onGloballyPositioned {
        autofillNode.boundingBox = it.boundsInWindow()
    }.onFocusChanged { focusState ->
        autofill?.run {
            if (focusState.isFocused) {
                requestAutofillForNode(autofillNode)
            } else {
                cancelAutofillForNode(autofillNode)
            }
        }
    }
}

@Composable
fun RegisterLink(
    onNavigateToRegister: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Two text fields displaed in a row
        Text(text = stringResource(resource = Res.string.register_prefix) + " ")
        Text(text = stringResource(resource = Res.string.register_link),
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onNavigateToRegister() })
    }
}