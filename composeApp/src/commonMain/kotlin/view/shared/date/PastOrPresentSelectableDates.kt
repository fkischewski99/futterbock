package view.shared.date

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// PastOrPresentSelectableDates.kt
@OptIn(ExperimentalMaterial3Api::class)
object PastOrPresentSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= Clock.System.now().toEpochMilliseconds()
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    }
}
