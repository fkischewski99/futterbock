package view.event.new_event

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import model.Meal
import view.shared.HelperFunctions

fun groupMealsByDate(
    from: Instant,
    to: Instant,
    mealList: List<Meal>
): Map<LocalDate, List<Meal>> {

    val updatedMap = mutableMapOf<LocalDate, MutableList<Meal>>()
    var currentDate =
        HelperFunctions.getLocalDate(from)
    val endLocalDate =
        HelperFunctions.getLocalDate(to)
    while (currentDate <= endLocalDate) {
        val mealsForCurrentDate =
            mealList
                .filter { it.day.toLocalDateTime(TimeZone.currentSystemDefault()).date == currentDate }
                .toMutableList()
        updatedMap[currentDate] = mealsForCurrentDate
        currentDate = currentDate.plus(DatePeriod(years = 0, months = 0, days = 1))
    }
    updatedMap.values.forEach { meals ->
        meals.sortBy { it.mealType }
    }
    return updatedMap

}
