package data.csv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FilePicker {
    actual suspend fun pickCsvFile(): FilePickerResult {
        return suspendCancellableCoroutine { continuation ->
            // This implementation requires Composable context
            // Use the Composable function instead
            continuation.resume(
                FilePickerResult(error = "Use rememberCsvFilePicker() Composable function for Android")
            )
        }
    }
}

@Composable
fun rememberCsvFilePicker(
    onResult: (FilePickerResult) -> Unit
): () -> Unit {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    val fileName = uri.lastPathSegment ?: "unknown.csv"
                    onResult(FilePickerResult(content = content, fileName = fileName))
                }
            } catch (e: Exception) {
                onResult(FilePickerResult(error = "Error reading file: ${e.message}"))
            }
        } else {
            onResult(FilePickerResult(error = "No file selected"))
        }
    }
    
    return {
        launcher.launch(arrayOf("text/csv", "text/plain", "application/csv"))
    }
}