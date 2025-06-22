package view.admin.csv_import

import data.csv.FilePickerResult
import view.event.actions.BaseAction

sealed class CsvImportActions : BaseAction {
    data class FileSelected(val result: FilePickerResult) : CsvImportActions()
    data class SetColumnMapping(
        val firstNameColumn: Int?,
        val lastNameColumn: Int?,
        val birthDateColumn: Int?
    ) : CsvImportActions()

    data object StartValidation : CsvImportActions()
    data object StartImport : CsvImportActions()
    data object Reset : CsvImportActions()
    data object Close : CsvImportActions()
}