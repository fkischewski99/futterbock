package view.admin.csv_import

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import data.csv.FilePickerResult

@Composable
expect fun PlatformFilePicker(
    onFileSelected: (FilePickerResult) -> Unit,
    modifier: Modifier = Modifier
)