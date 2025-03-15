package view.login

import EmailTextField
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import futterbock_app.composeapp.generated.resources.Res
import futterbock_app.composeapp.generated.resources.login_submit
import futterbock_app.composeapp.generated.resources.logo
import futterbock_app.composeapp.generated.resources.stamm
import futterbock_app.composeapp.generated.resources.startPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import services.login.LoginAndRegister
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Register(
    onRegisterNavigation: () -> Unit,
    onBackNavigation: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("") }
    var registerError by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    val isSubmitEnabled = remember(email, password, passwordConfirm, group, loading) {
        !loading && (email.isNotBlank() && password.isNotBlank() && passwordConfirm.isNotBlank() && group.isNotBlank())
    }

    val focusManager by rememberUpdatedState(LocalFocusManager.current)
    val scope = rememberCoroutineScope()
    val currentApp: LoginAndRegister = koinInject()

    fun onSubmit() {
        scope.launch {
            loading = true
            delay(5000)
            if (password != passwordConfirm) {
                registerError = "Passwörter stimmen nicht überein"
                loading = false
                return@launch
            }
            if (password.length < 6) {
                registerError = "Passwort muss mindestens 6 Zeichen lang sein"
                loading = false
                return@launch
            }
            try {
                currentApp.register(email = email, password = password, group = group)
                onRegisterNavigation()
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                registerError =
                    "Fehler beim Registrieren: Bitte geben Sie eine valide email an."
            } catch (e: FirebaseAuthUserCollisionException) {
                registerError =
                    "Fehler beim Registrieren: Der Username existiert bereits. Bitte verwenden Sie einen anderen Usernamen."
            } catch (e: Exception) {
                registerError =
                    "Ein unbekannter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut"
            } finally {
                loading = false
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize().imePadding(), topBar = {
        TopAppBar(title = { Text("Registrieren") },
            navigationIcon = { NavigationIconButton(onLeave = onBackNavigation) })
    }) {
        Column(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.startPage),
                contentDescription = "futterbock",
            )
            Spacer(modifier = Modifier.padding(8.dp))
            EmailTextField(
                email = email,
                onEmailChange = { value: String -> email = value },
                loading = loading,
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                },
            )
            Spacer(modifier = Modifier.padding(4.dp))
            GroupTextField(group = group,
                onStammChange = { value: String -> group = value },
                loading = loading,
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                })
            Spacer(modifier = Modifier.padding(4.dp))
            PasswordTextField(
                password = password,
                onPasswordChange = { value: String -> password = value },
                loading = loading,
            )
            Spacer(modifier = Modifier.padding(4.dp))
            PasswordTextField(
                password = passwordConfirm,
                onPasswordChange = { value: String -> passwordConfirm = value },
                loading = loading,
                passwordName = "Passwort bestätigen"
            )
            Spacer(modifier = Modifier.padding(8.dp))
            ErrorField(errorMessage = registerError)
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

        }
    }
}


@Composable
fun GroupTextField(
    group: String,
    onStammChange: (value: String) -> Unit,
    loading: Boolean,
    onNext: () -> Unit = {},
) {
    OutlinedTextField(
        value = group,
        onValueChange = onStammChange,
        label = { Text(text = stringResource(Res.string.stamm)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
        ),
        enabled = !loading,
    )
}