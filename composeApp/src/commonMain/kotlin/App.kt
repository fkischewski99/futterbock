import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import data.AppMode
import data.AppModeHolder
import data.sync.InitialSyncService
import data.sync.SyncManager
import modules.dataModules
import modules.serviceModules
import modules.viewModelModules
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import services.pdfService.PdfServiceImpl
import view.navigation.RootNavController
import services.pdfService.PdfServiceModule
import view.theme.AppTheme


@Composable
fun App(pdfService: PdfServiceImpl) {
    KoinApplication(application = {
        modules(
            serviceModules, dataModules, viewModelModules
        )
    }) {
        val pdfServiceModule: PdfServiceModule = koinInject()
        pdfServiceModule.setPdfService(pdfService)

        val appModeHolder = koinInject<AppModeHolder>()
        val syncManager = koinInject<SyncManager>()
        val initialSyncService = koinInject<InitialSyncService>()
        LaunchedEffect(Unit) {
            if (appModeHolder.mode.value != AppMode.OFFLINE_ONLY) {
                syncManager.startObserving()
                initialSyncService.syncAllUserData()
            }
        }

        AppTheme {
            RootNavController()
        }
    }
}
