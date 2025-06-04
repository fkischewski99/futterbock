package services.pdfService

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import model.Material
import model.ShoppingIngredient
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSAttributedString
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSMutableData
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.UIKit.NSFontAttributeName
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginPDFContextToData
import platform.UIKit.UIGraphicsBeginPDFPage
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.drawInRect

private const val PDF_PAGE_WIDTH = 595.0
private const val PDF_PAGE_HEIGHT = 842.0

actual class PdfServiceImpl {


    private var pdfDocument: PDFDocument? = null
    private var XPOS = 80.0
    private val fileName = "Einkaufsliste.pdf"
    private val documentsDirectory = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).firstOrNull() as NSString
    private val pdfFilePath = documentsDirectory.stringByAppendingPathComponent(fileName)
    private val pdfFileURL = NSURL.fileURLWithPath(pdfFilePath)

    @OptIn(ExperimentalForeignApi::class)
    actual fun createPdf(
        shoppingList: Map<String, List<ShoppingIngredient>>,
        materialList: List<Material>
    ) {
        val pdfData = NSMutableData()

        // Define PDF context
        UIGraphicsBeginPDFContextToData(pdfData, CGRectMake(0.0, 0.0, 0.0, 0.0), null)

        // Start a new PDF page
        UIGraphicsBeginPDFPage()

        pdfDocument = PDFDocument() // Create a new PDF document
        // Start a new page for PDF
        val page = PDFPage()
        page.setBounds(CGRectMake(0.0, 0.0, PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT), 0)
        pdfDocument?.insertPage(page, atIndex = 0u)

        // Define some attributes
        val titleFontSize = 24.0
        val headingFontSize = 18.0
        val bodyFontSize = 12.0

        // Draw the title
        val title = "Einkaufsliste"
        drawTextOnPDF(title, titleFontSize, 50.0)
        var yPosition = 100.0 // Initial vertical position for content

        // Draw categories and ingredients
        for ((category, ingredients) in shoppingList) {
            // Draw category name
            drawTextOnPDF(
                category,
                headingFontSize,
                yPosition
            )
            yPosition += 30.0 // Move down for ingredients

            // Draw each ingredient
            for (ingredient in ingredients) {
                val ingredientText = "[ ] ${ingredient.ingredient!!.name}"
                drawTextOnPDF(
                    ingredientText,
                    bodyFontSize,
                    yPosition
                )
                yPosition += 20.0

                // Check for page overflow
                if (yPosition + 80 > PDF_PAGE_HEIGHT) { // Assuming a page height limit
                    UIGraphicsBeginPDFPage()
                    yPosition = 50.0 // Reset position on new page
                }
            }
            yPosition += 10.0 // Space between categories
        }
        UIGraphicsEndPDFContext()

        // Write PDF to file
        val fileURL = NSURL.fileURLWithPath(pdfFilePath)
        pdfData.writeToFile(fileURL.path!!, true)
    }

    actual fun sharePdf() {

        // Verify if file exists
        val fileManager = NSFileManager.defaultManager
        val fileExists = fileManager.fileExistsAtPath(pdfFilePath)

        if (!fileExists) {
            println("PDF file does not exist at path: $pdfFilePath")
            return
        }

        // Create share activity
        val activityViewController = UIActivityViewController(
            activityItems = listOf(pdfFileURL),
            applicationActivities = null
        )

        // Present the share sheet from the root view controller
        val currentViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        currentViewController?.presentViewController(activityViewController, true, null)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun drawTextOnPDF(
        inputText: String,
        fontSize: Double,
        yposition: Double
    ) {
        val font = UIFont.systemFontOfSize(fontSize)
        val titleAttributes: Map<Any?, *> =
            mapOf(NSFontAttributeName to font)
        // Calculate string size and position
        val nsAttributedString = NSAttributedString.create(inputText, titleAttributes)
        val position = createCGPoint(XPOS, yposition, fontSize + 5)
        nsAttributedString.drawInRect(rect = position)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun createCGPoint(x: Double, y: Double, height: Double = 20.0): CValue<CGRect> {
        // Create a native CGPoint
        return CGRectMake(x, y, height = height, width = 400.0)
    }
}
