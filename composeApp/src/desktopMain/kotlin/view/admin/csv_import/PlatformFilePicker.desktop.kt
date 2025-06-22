package view.admin.csv_import

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import data.csv.FilePicker
import data.csv.FilePickerResult
import kotlinx.coroutines.launch

@Composable
actual fun PlatformFilePicker(
    onFileSelected: (FilePickerResult) -> Unit,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()
    val filePicker = FilePicker()
    
    Button(
        onClick = {
            scope.launch {
                val result = filePicker.pickCsvFile()
                onFileSelected(result)
            }
        },
        modifier = modifier
    ) {
        Text("Datei auswählen")
    }
}