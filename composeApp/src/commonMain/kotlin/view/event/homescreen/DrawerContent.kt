package view.event.homescreen

import ConfirmDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.EventRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import services.login.LoginAndRegister

@Composable
fun DrawerContent(
    onClose: () -> Unit,
    onLogoutNavigation: () -> Unit
) {
    val login: LoginAndRegister = koinInject()
    var showConfirmDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Spacer(modifier = Modifier.height(16.dp))
    ModalDrawerSheet {
        Text("Benutzer", modifier = Modifier.padding(16.dp))
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text(text = "Logout") },
            selected = false,
            onClick = {
                scope.launch {
                    login.logout()
                    onLogoutNavigation()
                }
            }
        )
        NavigationDrawerItem(
            label = { Text(text = "Account löschen", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = {
                scope.launch {
                    showConfirmDialog = true
                }
            }
        )
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text(text = "Schließen") },
            selected = false,
            onClick = onClose
        )
    }
    if (showConfirmDialog) {
        ConfirmDialog(
            onConfirm = {
                scope.launch {
                    login.deleteCurrentUser()
                    onLogoutNavigation()
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}
