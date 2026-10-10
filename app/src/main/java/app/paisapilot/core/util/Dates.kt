package app.paisapilot.core.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)

fun LocalDate.short(): String = format(dayMonth)

fun LocalDate.pretty(): String = format(dayMonthYear)

fun YearMonth.pretty(): String = format(monthYear)

/** Day-of-month clamped to the month's length, so "31" works in February. */
fun YearMonth.atDayClamped(day: Int): LocalDate = atDay(day.coerceIn(1, lengthOfMonth()))

/** The next date falling on [day] of a month, on or after [from] (strictly after when not [inclusive]). */
fun nextDayOfMonth(day: Int, from: LocalDate, inclusive: Boolean = true): LocalDate {
    val thisMonth = YearMonth.from(from).atDayClamped(day)
    val ok = if (inclusive) !thisMonth.isBefore(from) else thisMonth.isAfter(from)
    return if (ok) thisMonth else YearMonth.from(from).plusMonths(1).atDayClamped(day)
}

fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()

/** "Today", "Tomorrow", "in 5 days", "3 days overdue". */
fun dueLabel(due: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = daysBetween(today, due)
    return when {
        days == 0 -> "Due today"
        days == 1 -> "Due tomorrow"
        days > 1 -> "Due in $days days"
        days == -1 -> "1 day overdue"
        else -> "${abs(days)} days overdue"
    }
}

fun ordinal(day: Int): String {
    val suffix = when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}
