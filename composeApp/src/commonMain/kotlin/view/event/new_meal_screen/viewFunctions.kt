package view.event.new_meal_screen

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import model.ParticipantTime

fun generateDateRange(from: Instant, to: Instant): List<LocalDate> {
    val fromDateTime = from.toLocalDateTime(TimeZone.currentSystemDefault())
    val toDateTime = to.toLocalDateTime(TimeZone.currentSystemDefault())

    val dates = mutableListOf<LocalDate>()
    var currentDate = fromDateTime.date

    while (currentDate <= toDateTime.date) {
        dates.add(currentDate)
        currentDate = currentDate.plus(DatePeriod(years = 0, months = 0, days = 1))
    }
    return dates
}

