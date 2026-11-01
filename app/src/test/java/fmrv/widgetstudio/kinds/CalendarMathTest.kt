package fmrv.widgetstudio.kinds

import fmrv.widgetstudio.kinds.calendar.CalendarMath
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarMathTest {

    @Test
    fun `les cases vides avant le premier du mois dependent du debut de semaine`() {
        val nov2026 = LocalDate.of(2026, 11, 1) // dimanche
        assertEquals(6, CalendarMath.leadingBlanks(nov2026, DayOfWeek.MONDAY))
        assertEquals(0, CalendarMath.leadingBlanks(nov2026, DayOfWeek.SUNDAY))
        assertEquals(0, CalendarMath.leadingBlanks(LocalDate.of(2026, 6, 1), DayOfWeek.MONDAY)) // lundi
    }

    @Test
    fun `le nombre de semaines de la grille est correct`() {
        assertEquals(6, CalendarMath.weeksInGrid(6, 30)) // novembre 2026, lundi en premier : 36 cases
        assertEquals(5, CalendarMath.weeksInGrid(0, 30)) // 30 cases
        assertEquals(5, CalendarMath.weeksInGrid(1, 30)) // 31 cases
        assertEquals(4, CalendarMath.weeksInGrid(0, 28)) // février 2027, cas minimal
        assertEquals(6, CalendarMath.weeksInGrid(5, 31)) // 36 cases
    }

    @Test
    fun `chaque case affiche le bon jour ou reste vide`() {
        assertNull(CalendarMath.dayAt(0, 0, leadingBlanks = 6, daysInMonth = 30))
        assertEquals(1, CalendarMath.dayAt(0, 6, 6, 30))
        assertEquals(2, CalendarMath.dayAt(1, 0, 6, 30))
        assertEquals(24, CalendarMath.dayAt(4, 1, 6, 30))
        assertEquals(30, CalendarMath.dayAt(5, 0, 6, 30))
        assertNull(CalendarMath.dayAt(5, 1, 6, 30))
    }

    @Test
    fun `les jours restants sont positifs, nuls ou negatifs`() {
        val today = LocalDate.of(2026, 11, 1)
        assertEquals(30, CalendarMath.daysUntil(today, LocalDate.of(2026, 12, 1)))
        assertEquals(0, CalendarMath.daysUntil(today, today))
        assertEquals(-1, CalendarMath.daysUntil(today, LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `une date invalide ou vide donne null`() {
        assertEquals(LocalDate.of(2027, 1, 15), CalendarMath.parseDate("2027-01-15"))
        assertNull(CalendarMath.parseDate(""))
        assertNull(CalendarMath.parseDate("15/01/2027"))
        assertNull(CalendarMath.parseDate("2027-13-45"))
    }

    @Test
    fun `la progression reste entre 0 et 1 et suit l'heure`() {
        val noon = ZonedDateTime.of(2026, 11, 4, 12, 0, 0, 0, ZoneId.of("Europe/Paris")) // mercredi
        val p = CalendarMath.progress(noon)
        assertEquals(0.5f, p.day)
        assertEquals((2 + 0.5f) / 7f, p.week)
        assertEquals((3 + 0.5f) / 30f, p.month)
        assertEquals((307 + 0.5f) / 365f, p.year)

        val midnight = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Paris"))
        assertEquals(0f, CalendarMath.progress(midnight).year)
    }
}
