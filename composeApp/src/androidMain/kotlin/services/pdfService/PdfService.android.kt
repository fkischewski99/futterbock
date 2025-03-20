package services.pdfService

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.Page
import android.os.Environment
import android.provider.MediaStore
import kotlinx.datetime.Clock
import model.Material
import model.ShoppingIngredient
import java.io.IOException

/**Dimension For A4 Size Paper (1 inch = 72 points)**/
private const val PDF_PAGE_WIDTH = 595 //8.26 Inch
private const val PDF_PAGE_HEIGHT = 842 //11.69 Inch

actual class PdfServiceImpl(
    private val context: Context
) {
    private var currentDocument: PdfDocument? = null;

    actual fun createPdf(
        shoppingList: Map<String, List<ShoppingIngredient>>,
        materialList: List<Material>
    ) {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo =
            PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        drawHeadline("Einkaufsliste", canvas)
        var yPosition = 50F // Adjust yPosition for the next line


        shoppingList.forEach { entry ->
            yPosition += 50F
            if (checkForNewPage(entry.value, yPosition)) {
                page = getNextPage(document, page)
                canvas = page.canvas
                yPosition = 60F
            }
            drawCategoryHeadline(entry.key, canvas, yPosition)
            entry.value.forEach {
                yPosition += 20F // Move to the next line
                if (yPosition > PDF_PAGE_HEIGHT - 20) {
                    page = getNextPage(document, page)
                    canvas = page.canvas
                    yPosition = 60F
                }
                drawIngredient(it, canvas, yPosition)
            }
        }
        document.finishPage(page);
        currentDocument = document;
    }


    private fun getNextPage(pdfDoc: PdfDocument, previousPage: Page): Page {
        pdfDoc.finishPage(previousPage); //Closes Previous page
        val pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, 1).create();
        return pdfDoc.startPage(pageInfo); //Begins New page
    }

    private fun checkForNewPage(
        listOfIngredient: List<ShoppingIngredient>,
        yPosition: Float
    ): Boolean {
        return (yPosition + 80F + listOfIngredient.size * 20F > PDF_PAGE_HEIGHT)
    }

    actual fun sharePdf() {
        // Check if the document exists
        val document = currentDocument ?: throw IllegalStateException("No Document Created")

        // Define constants
        val fileName = "Einkaufsliste" + Clock.System.now().toEpochMilliseconds() + ".pdf"
        val mimeType = "application/pdf"

        // Prepare ContentValues for the PDF document
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val resolver = context.contentResolver

        // Insert the new file into MediaStore
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: throw IOException("Failed to create new media entry")

        // Write the PDF document to the output stream
        resolver.openOutputStream(uri)?.use { outputStream ->
            document.writeTo(outputStream)
        } ?: throw IOException("Failed to open output stream for URI: $uri")

        // Close the document after writing
        document.close()

        // Prepare the sharing Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri) // Attach the URI of the PDF
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Start the sharing activity
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
    }

    private fun drawIngredient(
        shoppingIngredient: ShoppingIngredient,
        canvas: Canvas,
        yPosition: Float
    ) {
        canvas.drawText("[  ] $shoppingIngredient", 80F, yPosition, Paint())
    }

    private fun drawHeadline(s: String, canvas: Canvas) {
        val paint = Paint()
        paint.textSize = 24f
        canvas.drawText(s, 80F, 50F, paint)
    }

    private fun drawCategoryHeadline(s: String, canvas: Canvas, yPosition: Float) {
        val paint = Paint()
        paint.textSize = 18f
        canvas.drawText(s, 80F, yPosition, paint)
    }


}