package data.csv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.coroutines.resume

actual class FilePicker {
    actual suspend fun pickCsvFile(): FilePickerResult {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val fileDialog = FileDialog(null as Frame?, "CSV-Datei auswählen", FileDialog.LOAD)
                    fileDialog.setFilenameFilter { _, name ->
                        name.lowercase().endsWith(".csv") || 
                        name.lowercase().endsWith(".txt")
                    }
                    fileDialog.isVisible = true
                    
                    val selectedFile = fileDialog.file
                    val selectedDirectory = fileDialog.directory
                    
                    if (selectedFile != null && selectedDirectory != null) {
                        val file = File(selectedDirectory, selectedFile)
                        if (file.exists() && file.canRead()) {
                            val content = file.readText(Charsets.UTF_8)
                            continuation.resume(
                                FilePickerResult(
                                    content = content,
                                    fileName = selectedFile
                                )
                            )
                        } else {
                            continuation.resume(
                                FilePickerResult(error = "Datei kann nicht gelesen werden")
                            )
                        }
                    } else {
                        continuation.resume(
                            FilePickerResult(error = "Keine Datei ausgewählt")
                        )
                    }
                } catch (e: Exception) {
                    continuation.resume(
                        FilePickerResult(error = "Fehler beim Öffnen der Datei: ${e.message}")
                    )
                }
            }
        }
    }
}