import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import data.AppMode
import data.AppModeHolder
import data.AppModePreferences
import data.sync.SyncManager
import modules.dataModules
import modules.serviceModules
import modules.viewModelModules
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.module
import services.pdfService.PdfServiceImpl
import view.navigation.RootNavController
import services.pdfService.PdfServiceModule
import view.theme.AppTheme


@Composable
fun App(pdfService: PdfServiceImpl) {
    val prefs = remember { AppModePreferences() }
    val appModeHolder = remember { AppModeHolder(prefs.getAppMode()) }

    val appModeModule = remember {
        module {
            single { appModeHolder }
            single { prefs }
        }
    }

    KoinApplication(application = {
        modules(
            appModeModule, serviceModules, dataModules, viewModelModules
        )
    }) {
        val pdfServiceModule: PdfServiceModule = koinInject()
        pdfServiceModule.setPdfService(pdfService)

        val syncManager = koinInject<SyncManager>()
        LaunchedEffect(Unit) {
            if (appModeHolder.mode.value == AppMode.OFFLINE_FIRST) {
                syncManager.startObserving()
            }
        }

        AppTheme {
            RootNavController()
        }
    }
}
