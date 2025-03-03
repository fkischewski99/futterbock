import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
            dataModules, viewModelModules, serviceModules
        )
    }) {
        val pdfServiceModule: PdfServiceModule = koinInject()

        pdfServiceModule.setPdfService(pdfService)

        AppTheme {
            RootNavController()
            //Navigator(NewParicipantScreen(null)) {navigator -> SlideTransition(navigator) }
        }
    }
}

@Composable
fun AppContent(pdfService: PdfServiceImpl) {


}
