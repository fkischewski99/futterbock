package data.csv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class FilePicker {
    // Strong reference to prevent GC before UIKit weak delegate callback fires
    private var currentDelegate: NSObject? = null

    actual suspend fun pickCsvFile(): FilePickerResult {
        return suspendCancellableCoroutine { continuation ->
            val contentTypes = listOf(
                UTTypeCommaSeparatedText,
                UTTypePlainText,
                UTTypeText
            )
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes)
            picker.allowsMultipleSelection = false

            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>
                ) {
                    currentDelegate = null
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    if (url == null) {
                        continuation.resume(FilePickerResult(error = "Keine Datei ausgewählt"))
                        return
                    }

                    val accessing = url.startAccessingSecurityScopedResource()
                    try {
                        @Suppress("CAST_NEVER_SUCCEEDS")
                        val content = NSString.stringWithContentsOfURL(
                            url = url,
                            encoding = NSUTF8StringEncoding,
                            error = null
                        ) as? String

                        if (content != null) {
                            continuation.resume(
                                FilePickerResult(
                                    content = content,
                                    fileName = url.lastPathComponent ?: "unknown.csv"
                                )
                            )
                        } else {
                            continuation.resume(
                                FilePickerResult(error = "Datei konnte nicht gelesen werden")
                            )
                        }
                    } finally {
                        if (accessing) {
                            url.stopAccessingSecurityScopedResource()
                        }
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    currentDelegate = null
                    continuation.resume(FilePickerResult(error = "Dateiauswahl abgebrochen"))
                }
            }

            currentDelegate = delegate
            picker.delegate = delegate

            continuation.invokeOnCancellation {
                currentDelegate = null
                picker.dismissViewControllerAnimated(true, completion = null)
            }

            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                rootViewController.presentViewController(picker, animated = true, completion = null)
            } else {
                currentDelegate = null
                continuation.resume(FilePickerResult(error = "Kein ViewController verfügbar"))
            }
        }
    }
}
