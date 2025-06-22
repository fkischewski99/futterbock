package data.csv

data class FilePickerResult(
    val content: String? = null,
    val fileName: String? = null,
    val error: String? = null
)

expect class FilePicker {
    suspend fun pickCsvFile(): FilePickerResult
}