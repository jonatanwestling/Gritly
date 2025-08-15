package se.umu.c22jwg.gritly.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * cleanDate is a function that formats a date string into a clean readable format.
 * It checks if the date is today, yesterday, or a specific date in the past and formats it accordingly.
 *
 * @param date The date string in ISO format (yyyy-MM-dd).
 * @return A human-readable string representing the date.
 *        Returns "Today" if the date is today, "Yesterday" if it was yesterday,
 *        or the formatted date string for any other past date.
 */
fun cleanDate(date: String): String {
    val date = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    val today = LocalDate.now()
    val pattern = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(pattern)
    }
}
