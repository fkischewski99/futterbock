package view.shared.date

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
class SelectedAbleDatesWithFromTo(val from: Instant, val to: Instant) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis >= from.toEpochMilliseconds() && utcTimeMillis <= to.toEpochMilliseconds()
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= from.toLocalDateTime(TimeZone.currentSystemDefault()).year
    }
}