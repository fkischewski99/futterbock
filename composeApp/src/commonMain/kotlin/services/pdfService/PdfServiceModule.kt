package services.pdfService

import org.koin.core.component.KoinComponent
import services.shoppingList.CalculateShoppingList
import services.shoppingList.groupIngredientByCategory

class PdfServiceModule(private val calculateShoppingList: CalculateShoppingList) : KoinComponent {
    private var pdfService: PdfServiceImpl? = null;

    fun setPdfService(pdfService: PdfServiceImpl) {
        this.pdfService = pdfService;
    }

    suspend fun createPdf(eventId: String) {
        if (pdfService == null) {
            return
        }
        val shoppingIngredients = calculateShoppingList.calculate(eventId)
        pdfService!!.createPdf(groupIngredientByCategory(shoppingIngredients));
        pdfService!!.sharePdf()
    }
}