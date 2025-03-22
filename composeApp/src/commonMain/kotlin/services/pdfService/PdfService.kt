package services.pdfService

import model.Material
import model.ShoppingIngredient


expect class PdfServiceImpl {
    fun createPdf(
        shoppingList: Map<String, List<ShoppingIngredient>>,
        materialList: List<Material>
    )

    fun sharePdf()
}