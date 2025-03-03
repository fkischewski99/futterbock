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

        fun formatDate(date: LocalDate): String {
            return "" + date.dayOfMonth + "." + date.monthNumber + "." + date.year;
        }

        fun formatDate(date: Instant): String {
            return formatLongDate(date.epochSeconds * 1000);
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
    }
}