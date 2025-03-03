import androidx.compose.ui.window.ComposeUIViewController
import services.pdfService.PdfServiceImpl

fun MainViewController() = ComposeUIViewController {
    val pdfService = PdfServiceImpl()
    App(pdfService)
}