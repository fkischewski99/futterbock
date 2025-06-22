package view.admin.csv_import

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.koin.compose.koinInject
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.shared.MGCircularProgressIndicator
import view.shared.ResultState

@Composable
fun CsvImportScreen(
    navController: NavHostController
) {
    val viewModel: CsvImportViewModel = koinInject()
    val state = viewModel.state.collectAsStateWithLifecycle()
    
    when (val currentState = state.value) {
        is ResultState.Success -> {
            CsvImportWizard(
                state = currentState.data,
                onFileSelected = { result ->
                    viewModel.onAction(CsvImportActions.FileSelected(result))
                },
                onColumnMappingChanged = { firstName, lastName, birthDate ->
                    viewModel.onAction(
                        CsvImportActions.SetColumnMapping(firstName, lastName, birthDate)
                    )
                },
                onStartValidation = {
                    viewModel.onAction(CsvImportActions.StartValidation)
                },
                onStartImport = {
                    viewModel.onAction(CsvImportActions.StartImport)
                },
                onReset = {
                    viewModel.onAction(CsvImportActions.Reset)
                },
                onClose = {
                    handleNavigation(navController, NavigationActions.GoBack)
                }
            )
        }
        
        is ResultState.Error -> {
            // Handle error state
            CsvImportWizard(
                state = ImportWizardState(),
                onFileSelected = { },
                onColumnMappingChanged = { _, _, _ -> },
                onStartValidation = { },
                onStartImport = { },
                onReset = { },
                onClose = {
                    handleNavigation(navController, NavigationActions.GoBack)
                }
            )
        }
        
        ResultState.Loading -> {
            MGCircularProgressIndicator()
        }
    }
}