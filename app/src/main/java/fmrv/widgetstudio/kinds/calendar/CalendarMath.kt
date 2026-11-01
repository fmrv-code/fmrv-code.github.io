package fmrv.widgetstudio.kinds.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Calculs de calendrier purs, testables sur JVM. */
object CalendarMath {

    /** Nombre de cases vides avant le 1er du mois, selon le premier jour de la semaine. */
    fun leadingBlanks(firstOfMonth: LocalDate, weekStart: DayOfWeek): Int =
        (firstOfMonth.dayOfWeek.value - weekStart.value + 7) % 7

    fun weeksInGrid(leadingBlanks: Int, daysInMonth: Int): Int = (leadingBlanks + daysInMonth + 6) / 7

    /** Jour du mois affiché dans la case [week]/[column], ou `null` pour une case vide. */
    fun dayAt(week: Int, column: Int, leadingBlanks: Int, daysInMonth: Int): Int? {
        val day = week * 7 + column - leadingBlanks + 1
        return day.takeIf { it in 1..daysInMonth }
    }

    /** Jours restants jusqu'à [target] (négatif si la date est passée). */
    fun daysUntil(today: LocalDate, target: LocalDate): Long = ChronoUnit.DAYS.between(today, target)

    /** Dates au format AAAA-MM-JJ ; `null` si le texte est vide ou invalide. */
    fun parseDate(text: String): LocalDate? = try {
        if (text.isBlank()) null else LocalDate.parse(text.trim())
    } catch (e: java.time.format.DateTimeParseException) {
        null
    }

    /** Progression (0..1) de la journée, la semaine (lundi = début), le mois et l'année en cours. */
    data class Progress(val day: Float, val week: Float, val month: Float, val year: Float)

    fun progress(now: ZonedDateTime): Progress {
        val secondsToday = now.toLocalTime().toSecondOfDay()
        val dayFraction = secondsToday / 86_400f
        val date = now.toLocalDate()
        return Progress(
            day = dayFraction,
            week = ((date.dayOfWeek.value - 1) + dayFraction) / 7f,
            month = ((date.dayOfMonth - 1) + dayFraction) / date.lengthOfMonth(),
            year = ((date.dayOfYear - 1) + dayFraction) / date.lengthOfYear(),
        )
    }
}
