package view.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class HelperFunctions {
    companion object {

        fun generateRandomStringId(length: Int = 20): String {
            val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }

        fun getMillisInUTC(realmInstant: Instant?): Long {
            if (realmInstant != null) {
                return realmInstant.epochSeconds * 1000
            }
            return Clock.System.now().toEpochMilliseconds()
        }

        fun getInstant(date: LocalDate): Instant {
            return date.atStartOfDayIn(TimeZone.UTC)
        }

        fun formatLongDate(time: Long): String {
            val date = Instant.fromEpochMilliseconds(time)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            return formatDate(date)
        }

        fun getLocalDate(instant: Instant): LocalDate {
            return Instant.fromEpochSeconds(
                instant.epochSeconds,
                instant.nanosecondsOfSecond
            )
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        }

        fun getCurrentLocalDate(): LocalDate {
            return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }

        fun formatDate(date: LocalDate): String {
            return "" + date.dayOfMonth + "." + date.monthNumber + "." + date.year;
        }

        fun dateToNumberString(date: LocalDate): String {
            return "" + date.dayOfMonth.toString().padStart(2, '0') + date.monthNumber.toString()
                .padStart(2, '0') + date.year;
        }

        fun formatDate(date: Instant): String {
            return formatLongDate(date.toEpochMilliseconds());
        }

        /**
         * Instant at the start of the day
         */
        fun getInstant(date: Long): Instant {
            // Convert Long timestamp to Instant
            val instant = Instant.fromEpochMilliseconds(date)

            // Convert Instant to LocalDate using the system default time zone
            val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            return getInstant(localDate)

        }

        fun parseDate(dateString: String): Instant {
            require(dateString.length == 8) { "Invalid date format" }

            val day = dateString.substring(0, 2).toInt()
            val month = dateString.substring(2, 4).toInt()
            val year = dateString.substring(4, 8).toInt()

            val localDate = LocalDate(year, month, day)
            return localDate.atStartOfDayIn(TimeZone.currentSystemDefault())
        }
    }
}