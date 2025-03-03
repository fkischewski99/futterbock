package services.pdfService

import model.ShoppingIngredient


expect class PdfServiceImpl {
    fun createPdf(shoppingList: Map<String, List<ShoppingIngredient>>)
    fun sharePdf()
}