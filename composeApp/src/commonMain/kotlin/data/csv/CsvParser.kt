package data.csv

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import model.EatingHabit
import model.Participant

data class CsvData(
    val headers: List<String>,
    val rows: List<List<String>>,
    val delimiter: String
)

data class CsvParseResult(
    val data: CsvData? = null,
    val error: String? = null
)

data class CsvRow(
    val rowIndex: Int,
    val values: List<String>
)

class CsvParser {
    
    fun parseCSV(content: String): CsvParseResult {
        if (content.isBlank()) {
            return CsvParseResult(error = "CSV-Datei ist leer")
        }
        
        val delimiter = detectDelimiter(content)
        val lines = content.lines().filter { it.isNotBlank() }
        
        if (lines.isEmpty()) {
            return CsvParseResult(error = "CSV-Datei enthält keine gültigen Zeilen")
        }
        
        try {
            val parsedLines = lines.map { line ->
                parseCSVLine(line, delimiter)
            }
            
            // Check if first row could be headers (non-numeric content)
            val hasHeaders = parsedLines.isNotEmpty() && 
                parsedLines[0].any { value -> 
                    value.isNotEmpty() && !value.matches(Regex("\\d+")) 
                }
            
            val headers = if (hasHeaders && parsedLines.isNotEmpty()) {
                parsedLines[0]
            } else {
                generateDefaultHeaders(parsedLines.firstOrNull()?.size ?: 3)
            }
            
            val dataRows = if (hasHeaders && parsedLines.size > 1) {
                parsedLines.drop(1)
            } else {
                parsedLines
            }
            
            return CsvParseResult(
                data = CsvData(
                    headers = headers,
                    rows = dataRows,
                    delimiter = delimiter
                )
            )
            
        } catch (e: Exception) {
            return CsvParseResult(error = "Fehler beim Parsen der CSV-Datei: ${e.message}")
        }
    }
    
    private fun detectDelimiter(content: String): String {
        val sampleLines = content.lines().take(5).filter { it.isNotBlank() }
        val delimiters = listOf(",", ";", "\t")
        
        return delimiters.maxByOrNull { delimiter ->
            sampleLines.sumOf { line ->
                line.count { it.toString() == delimiter }
            }
        } ?: ","
    }
    
    private fun parseCSVLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote
                        current.append('"')
                        i++ // Skip next quote
                    } else {
                        // Start or end of quoted field
                        inQuotes = !inQuotes
                    }
                }
                char.toString() == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        
        result.add(current.toString().trim())
        return result
    }
    
    private fun generateDefaultHeaders(columnCount: Int): List<String> {
        return (1..columnCount).map { "Spalte $it" }
    }
}

data class ParticipantImportData(
    val firstName: String,
    val lastName: String,
    val birthDate: Instant? = null,
    val eatingHabit: EatingHabit = EatingHabit.OMNIVORE,
    val cookingGroup: String = "",
    val rowIndex: Int
)

data class ValidationError(
    val rowIndex: Int,
    val field: String,
    val message: String
)

data class ValidationResult(
    val validParticipants: List<ParticipantImportData>,
    val errors: List<ValidationError>,
    val duplicates: List<ParticipantImportData>
)

class ParticipantCsvValidator {
    
    fun validateParticipantData(
        csvData: CsvData,
        firstNameColumn: Int,
        lastNameColumn: Int,
        birthDateColumn: Int? = null,
        eatingHabitColumn: Int? = null,
        cookingGroupColumn: Int? = null
    ): ValidationResult {
        val validParticipants = mutableListOf<ParticipantImportData>()
        val errors = mutableListOf<ValidationError>()
        val seen = mutableSetOf<String>()
        val duplicates = mutableListOf<ParticipantImportData>()
        
        csvData.rows.forEachIndexed { index, row ->
            val rowNumber = index + 1
            
            // Validate required columns exist
            if (firstNameColumn >= row.size) {
                errors.add(ValidationError(rowNumber, "Vorname", "Spalte nicht gefunden"))
                return@forEachIndexed
            }
            if (lastNameColumn >= row.size) {
                errors.add(ValidationError(rowNumber, "Nachname", "Spalte nicht gefunden"))
                return@forEachIndexed
            }
            
            val firstName = row[firstNameColumn].trim()
            val lastName = row[lastNameColumn].trim()
            
            // Validate required fields
            if (firstName.isEmpty()) {
                errors.add(ValidationError(rowNumber, "Vorname", "Vorname darf nicht leer sein"))
            }
            if (lastName.isEmpty()) {
                errors.add(ValidationError(rowNumber, "Nachname", "Nachname darf nicht leer sein"))
            }
            
            if (firstName.isEmpty() || lastName.isEmpty()) {
                return@forEachIndexed
            }
            
            // Parse birth date if column is provided
            var birthDate: Instant? = null
            if (birthDateColumn != null && birthDateColumn < row.size) {
                val birthDateStr = row[birthDateColumn].trim()
                if (birthDateStr.isNotEmpty()) {
                    birthDate = parseBirthDate(birthDateStr)
                    if (birthDate == null) {
                        errors.add(ValidationError(rowNumber, "Geburtsdatum", "Ungültiges Datumsformat. Erwartet: YYYY-MM-DD, DD.MM.YYYY oder DD/MM/YYYY"))
                        return@forEachIndexed
                    }
                }
            }
            
            // Parse eating habit if column is provided
            var eatingHabit = EatingHabit.OMNIVORE
            if (eatingHabitColumn != null && eatingHabitColumn < row.size) {
                val eatingHabitStr = row[eatingHabitColumn].trim()
                if (eatingHabitStr.isNotEmpty()) {
                    eatingHabit = parseEatingHabit(eatingHabitStr)
                        ?: run {
                            errors.add(ValidationError(rowNumber, "Essgewohnheit", "Ungültige Essgewohnheit. Erwartet: Vegan, Vegetarisch, Pescetarisch oder Omnivore"))
                            return@forEachIndexed
                        }
                }
            }
            
            // Parse cooking group if column is provided
            var cookingGroup = ""
            if (cookingGroupColumn != null && cookingGroupColumn < row.size) {
                val cookingGroupStr = row[cookingGroupColumn].trim()
                if (cookingGroupStr.isNotEmpty()) {
                    cookingGroup = cookingGroupStr
                }
            }
            
            val participant = ParticipantImportData(
                firstName = firstName,
                lastName = lastName,
                birthDate = birthDate,
                eatingHabit = eatingHabit,
                cookingGroup = cookingGroup,
                rowIndex = rowNumber
            )
            
            // Check for duplicates within CSV
            val key = "${firstName.lowercase()}_${lastName.lowercase()}"
            if (seen.contains(key)) {
                duplicates.add(participant)
            } else {
                seen.add(key)
                validParticipants.add(participant)
            }
        }
        
        return ValidationResult(
            validParticipants = validParticipants,
            errors = errors,
            duplicates = duplicates
        )
    }
    
    private fun parseBirthDate(dateStr: String): Instant? {
        val patterns = listOf(
            // YYYY-MM-DD
            Regex("""(\d{4})-(\d{1,2})-(\d{1,2})"""),
            // DD.MM.YYYY or DD/MM/YYYY
            Regex("""(\d{1,2})[./](\d{1,2})[./](\d{4})"""),
            // MM/DD/YYYY
            Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""")
        )
        
        for (pattern in patterns) {
            val match = pattern.matchEntire(dateStr.trim())
            if (match != null) {
                val groups = match.groupValues
                return try {
                    when (pattern) {
                        patterns[0] -> { // YYYY-MM-DD
                            val year = groups[1].toInt()
                            val month = groups[2].toInt()
                            val day = groups[3].toInt()
                            LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC)
                        }
                        patterns[1] -> { // DD.MM.YYYY or DD/MM/YYYY
                            val day = groups[1].toInt()
                            val month = groups[2].toInt()
                            val year = groups[3].toInt()
                            LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC)
                        }
                        patterns[2] -> { // MM/DD/YYYY
                            val month = groups[1].toInt()
                            val day = groups[2].toInt()
                            val year = groups[3].toInt()
                            LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC)
                        }
                        else -> null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        return null
    }
    
    private fun parseEatingHabit(habitStr: String): EatingHabit? {
        val normalized = habitStr.trim().lowercase()
        
        return when {
            normalized.contains("vegan") -> EatingHabit.VEGAN
            normalized.contains("vegetar") -> EatingHabit.VEGETARISCH
            normalized.contains("pescet") || normalized.contains("pesket") -> EatingHabit.PESCETARISCH
            normalized.contains("omniv") || normalized.contains("allesesser") || normalized.contains("fleisch") -> EatingHabit.OMNIVORE
            normalized == "v" -> EatingHabit.VEGAN
            normalized == "veg" -> EatingHabit.VEGETARISCH
            normalized == "pesc" -> EatingHabit.PESCETARISCH
            normalized == "omni" || normalized == "o" -> EatingHabit.OMNIVORE
            else -> null
        }
    }
}