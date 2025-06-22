package view.admin.csv_import

import data.csv.FilePickerResult
import view.event.actions.BaseAction

sealed class CsvImportActions : BaseAction {
    data class FileSelected(val result: FilePickerResult) : CsvImportActions()
    data class SetColumnMapping(
        val firstNameColumn: Int?,
        val lastNameColumn: Int?,
        val birthDateColumn: Int?,
        val eatingHabitColumn: Int?
    ) : CsvImportActions()

    data object StartValidation : CsvImportActions()
    data object StartImport : CsvImportActions()
    data object CancelImport : CsvImportActions()
    data object GoBack : CsvImportActions()
    data object Reset : CsvImportActions()
    data object Close : CsvImportActions()
}