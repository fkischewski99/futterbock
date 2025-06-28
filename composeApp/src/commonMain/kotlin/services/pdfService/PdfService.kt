package services.pdfService

import kotlinx.datetime.LocalDate
import model.Material
import model.Meal
import model.MultiDayShoppingList
import model.ShoppingIngredient


expect class PdfServiceImpl {
    fun createMultiDayShoppingListPdf(
        multiDayShoppingList: MultiDayShoppingList,
        materialList: List<Material>
    )

    fun createRecipePlanPdf(
        eventName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        mealsGroupedByDate: Map<LocalDate, List<Meal>>
    )

    fun sharePdf(filename: String = "Document.pdf")
}