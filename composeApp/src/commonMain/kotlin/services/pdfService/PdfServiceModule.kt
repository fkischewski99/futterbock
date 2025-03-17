package services.pdfService

import org.koin.core.component.KoinComponent
import services.materiallist.CalculateMaterialList
import services.shoppingList.CalculateShoppingList
import services.shoppingList.groupIngredientByCategory

class PdfServiceModule(
    private val calculateShoppingList: CalculateShoppingList,
    private val calculateMaterialList: CalculateMaterialList
) : KoinComponent {
    private var pdfService: PdfServiceImpl? = null;

    fun setPdfService(pdfService: PdfServiceImpl) {
        this.pdfService = pdfService;
    }

    suspend fun createPdf(eventId: String) {
        if (pdfService == null) {
            return
        }
        val shoppingIngredients = calculateShoppingList.calculate(eventId)
        val materialList = calculateMaterialList.calculate(eventId)
        pdfService!!.createPdf(groupIngredientByCategory(shoppingIngredients), materialList);
        pdfService!!.sharePdf()
    }
}