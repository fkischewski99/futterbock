package services.pdfService

import org.koin.core.component.KoinComponent
import services.materiallist.CalculateMaterialList
import services.shoppingList.CalculateShoppingList

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
        // Use multi-day calculation for optimal shopping day planning
        val multiDayShoppingList =
            calculateShoppingList.calculateMultiDay(eventId, saveToRepository = false)
        val materialList = calculateMaterialList.calculate(eventId)

        // Use the new multi-day PDF creation function
        pdfService!!.createMultiDayShoppingListPdf(multiDayShoppingList, materialList)
        pdfService!!.sharePdf("Einkaufsliste.pdf")
    }

    suspend fun createRecipePlanPdf(
        eventName: String,
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate,
        mealsGroupedByDate: Map<kotlinx.datetime.LocalDate, List<model.Meal>>
    ) {
        if (pdfService == null) {
            return
        }
        pdfService!!.createRecipePlanPdf(eventName, startDate, endDate, mealsGroupedByDate)
        pdfService!!.sharePdf("Wochenplan.pdf")
    }
}