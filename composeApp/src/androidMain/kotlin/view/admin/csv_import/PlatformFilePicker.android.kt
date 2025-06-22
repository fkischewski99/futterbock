package view.admin.csv_import

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import data.csv.FilePickerResult

@Composable
actual fun PlatformFilePicker(
    onFileSelected: (FilePickerResult) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    val fileName = uri.lastPathSegment ?: "unknown.csv"
                    onFileSelected(FilePickerResult(content = content, fileName = fileName))
                }
            } catch (e: Exception) {
                onFileSelected(FilePickerResult(error = "Error reading file: ${e.message}"))
            }
        } else {
            onFileSelected(FilePickerResult(error = "No file selected"))
        }
    }
    
    Button(
        onClick = {
            launcher.launch(arrayOf("text/csv", "text/plain", "application/csv"))
        },
        modifier = modifier
    ) {
        Text("Datei auswählen")
    }
}