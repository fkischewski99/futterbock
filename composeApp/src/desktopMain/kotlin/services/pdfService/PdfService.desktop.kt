package services.pdfService

import model.ShoppingIngredient
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter


/** Dimension For A4 Size Paper (1 inch = 72 points) **/
private const val PDF_PAGE_WIDTH = 595 // 8.26 Inch
private const val PDF_PAGE_HEIGHT = 842 // 11.69 Inch

actual class PdfServiceImpl {
    var currentDocument: PDDocument? = null
    var fontName = Standard14Fonts.getMappedFontName("Helvetica")
    var pdfFont: PDFont = PDType1Font(fontName)
    val fontNameHeading = Standard14Fonts.getMappedFontName("Helvetica-Bold")
    val pdfFontHeading: PDFont = PDType1Font(fontNameHeading)


    var contentStream: PDPageContentStream? = null
    var document: PDDocument? = null
    var yPosition = PDF_PAGE_HEIGHT - 100F


    actual fun createPdf(
        shoppingList: Map<String, List<ShoppingIngredient>>,
        materialList: Map<String, Int>
    ) {
        document = PDDocument()
        var page = PDPage()
        document!!.addPage(page)

        contentStream = PDPageContentStream(document, page)

        // Draw the heading
        setTitleOfCurrentPage("Einkaufsliste")

        // Draw category headers and ingredients
        for ((category, ingredients) in shoppingList) {
            contentStream!!.beginText()
            contentStream!!.newLineAtOffset(50f, yPosition)
            contentStream!!.setFont(pdfFontHeading, 18f)
            contentStream!!.showText(category)
            contentStream!!.endText()

            yPosition -= 20f // Move down for ingredients

            contentStream!!.setFont(pdfFont, 12f) // Reset font for ingredients
            ingredients.forEach {
                contentStream!!.beginText()
                contentStream!!.newLineAtOffset(80f, yPosition)
                contentStream!!.showText("[  ] $it") // Print each ingredient
                contentStream!!.endText()
                yPosition -= 20f // Move to the next line

                // Check for page overflow
                checkForOverflowAndCreateNewPage(page, pdfFont)
            }

            // Add space between categories
            yPosition -= 10f
        }

        createMaterialList(materialList = materialList)


        contentStream!!.close()
        currentDocument = document
    }

    private fun checkForOverflowAndCreateNewPage(
        page: PDPage,
        pdfFont: PDFont
    ) {
        var page1 = page
        if (yPosition < 40f) { // Create a new page if there's no space left
            page1 = PDPage()
            document!!.addPage(page1)
            contentStream!!.close()
            contentStream =
                PDPageContentStream(document, page1) // Create a new content stream
            contentStream!!.setFont(pdfFont, 12f) // Reset font on new page
            yPosition = PDF_PAGE_HEIGHT - 100F // Reset yPosition for new page
        }
    }

    private fun setTitleOfCurrentPage(
        title: String
    ) {
        contentStream!!.setFont(pdfFontHeading, 24f)

        contentStream!!.beginText()
        contentStream!!.newLineAtOffset(80f, PDF_PAGE_HEIGHT - 100F)
        contentStream!!.showText(title)
        contentStream!!.endText()

        yPosition = PDF_PAGE_HEIGHT - 150F // Adjust yPosition for the next line
        contentStream!!.setFont(pdfFont, 12f)
    }

    private fun createMaterialList(
        materialList: Map<String, Int>,
    ) {
        var page = PDPage()
        document!!.addPage(page)
        contentStream!!.close()
        contentStream =
            PDPageContentStream(document, page) // Create a new content stream
        setTitleOfCurrentPage("Materialliste")
        contentStream!!.setFont(pdfFont, 12f)

        materialList.forEach { (material, count) ->
            contentStream!!.beginText()
            contentStream!!.newLineAtOffset(80f, yPosition)
            contentStream!!.showText("[  ] ${count}x $material") // Print each ingredient
            contentStream!!.endText()
            yPosition -= 20f // Move to the next line

            // Check for page overflow
            checkForOverflowAndCreateNewPage(page, pdfFont)
        }
        contentStream!!.close()
    }

    actual fun sharePdf() {
        if (currentDocument == null) {
            throw Exception("No Document Created")
        }

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        // Use file chooser to save PDF
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Save PDF"
            fileFilter = FileNameExtensionFilter("PDF Documents", "pdf")
            selectedFile = File("Einkaufsliste.pdf")
        }

        val userSelection = fileChooser.showSaveDialog(null)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            var pdfFile = fileChooser.selectedFile
            if (!pdfFile.name.lowercase().endsWith(".pdf")) {
                pdfFile = File("${pdfFile.absolutePath}.pdf")
            }

            try {
                currentDocument!!.save(pdfFile)
                currentDocument!!.close()

                // Open the PDF file if the desktop supports it
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile)
                } else {
                    println("Desktop is not supported, can't open the file automatically.")
                }
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    null,
                    "Error saving the PDF: ${e.message}",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        } else {
            JOptionPane.showMessageDialog(
                null,
                "Save operation was cancelled",
                "Info",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }
}