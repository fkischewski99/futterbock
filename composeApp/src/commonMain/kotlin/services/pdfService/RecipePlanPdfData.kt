package services.pdfService

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import model.Meal
import model.MealType

/**
 * Data structure for organizing recipe plan PDF content
 */
data class RecipePlanPdfData(
    val title: String,
    val dateRange: String,
    val dailySections: List<DaySection>
)

data class DaySection(
    val dayHeader: String,
    val mealSections: List<MealSection>
)

data class MealSection(
    val mealTypeHeader: String,
    val recipes: List<String>
)

/**
 * Shared logic for processing recipe plan data into PDF-ready format
 */
object RecipePlanPdfProcessor {
    
    fun processRecipePlanData(
        eventName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        mealsGroupedByDate: Map<LocalDate, List<Meal>>
    ): RecipePlanPdfData {
        
        val dateFormat = LocalDate.Format {
            dayOfMonth()
            char('.')
            char(' ')
            monthNumber()
            char('.')
            char(' ')
            year()
        }
        
        val title = "WOCHENPLAN"
        val dateRange = "" // Remove date range from PDF
        
        val dailySections = mealsGroupedByDate.toList().sortedBy { it.first }.map { (date, meals) ->
            createDaySection(date, meals)
        }
        
        return RecipePlanPdfData(
            title = title,
            dateRange = dateRange,
            dailySections = dailySections
        )
    }
    
    private fun createDaySection(date: LocalDate, meals: List<Meal>): DaySection {
        // Simple date formatting without dayOfWeek names which requires parameters
        val dayHeader = "${date.dayOfMonth}.${date.monthNumber}.${date.year}"
        
        // Group meals by type for this day
        val mealsByType = meals.groupBy { it.mealType }
        
        // Display meals in order: Frühstück, Mittag, Abendessen, Snack
        val mealOrder = listOf(MealType.FRÜHSTÜCK, MealType.MITTAG, MealType.ABENDESSEN, MealType.SNACK)
        
        val mealSections = mealOrder.mapNotNull { mealType ->
            val mealsOfType = mealsByType[mealType]
            if (mealsOfType != null && mealsOfType.isNotEmpty()) {
                createMealSection(mealType, mealsOfType)
            } else {
                null
            }
        }
        
        return DaySection(
            dayHeader = dayHeader,
            mealSections = mealSections
        )
    }
    
    private fun createMealSection(mealType: MealType, meals: List<Meal>): MealSection {
        val mealTypeHeader = "• ${mealType.stringValue}"
        
        val recipes = meals.flatMap { meal ->
            meal.recipeSelections.map { recipeSelection ->
                "    - ${recipeSelection.selectedRecipeName} (für ${recipeSelection.eaterIds.size} Personen)"
            }
        }
        
        return MealSection(
            mealTypeHeader = mealTypeHeader,
            recipes = recipes
        )
    }
}