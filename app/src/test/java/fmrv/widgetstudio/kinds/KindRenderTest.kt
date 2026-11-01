package fmrv.widgetstudio.kinds

import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.WidgetStyle
import fmrv.widgetstudio.kinds.apps.AppFolderKind
import fmrv.widgetstudio.kinds.apps.AppIconKind
import fmrv.widgetstudio.kinds.apps.AppPillKind
import fmrv.widgetstudio.kinds.apps.SearchBarKind
import fmrv.widgetstudio.kinds.calendar.BigDateKind
import fmrv.widgetstudio.kinds.calendar.CountdownKind
import fmrv.widgetstudio.kinds.calendar.MonthCalendarKind
import fmrv.widgetstudio.kinds.calendar.ProgressKind
import fmrv.widgetstudio.kinds.calendar.WeekStripKind
import fmrv.widgetstudio.kinds.clocks.AnalogClockKind
import fmrv.widgetstudio.kinds.clocks.DigitalClockKind
import fmrv.widgetstudio.kinds.clocks.StackedClockKind
import fmrv.widgetstudio.kinds.clocks.WorldClockKind
import fmrv.widgetstudio.render.AnalogClock
import fmrv.widgetstudio.render.AnalogFace
import fmrv.widgetstudio.render.Box
import fmrv.widgetstudio.render.ClockText
import fmrv.widgetstudio.render.Click
import fmrv.widgetstudio.render.Column
import fmrv.widgetstudio.render.HAlign
import fmrv.widgetstudio.render.Img
import fmrv.widgetstudio.render.ImgSource
import fmrv.widgetstudio.render.Node
import fmrv.widgetstudio.render.Row
import fmrv.widgetstudio.render.Text
import fmrv.widgetstudio.render.imageRequests
import fmrv.widgetstudio.render.walk
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Vérifie la structure des arbres produits, sans Android : mercredi 4 novembre 2026, 10 h 30. */
class KindRenderTest {

    private val accent = 0xFFFF6B4A.toInt()
    private val now = ZonedDateTime.of(2026, 11, 4, 10, 30, 0, 0, ZoneId.of("Europe/Paris"))

    private fun env(kind: WidgetKind<*>, w: Float, h: Float, vararg overrides: Pair<String, String>) = RenderEnv(
        widthDp = w,
        heightDp = h,
        style = WidgetStyle(accentColor = accent),
        params = Params(kind.defaultParams + overrides),
        name = "Nom",
        now = now,
        locale = Locale.FRANCE,
        strings = { "s$it" },
    )

    private fun WidgetKind<Unit>.draw(env: RenderEnv, data: Unit = Unit): Node = render(env, data)

    private fun Node.texts() = walk().filterIsInstance<Text>().map { it.text }.toList()

    private fun Node.clocks() = walk().filterIsInstance<ClockText>().toList()

    // ---- Horloges

    @Test
    fun `l'horloge numerique affiche l'heure et la date`() {
        val clocks = DigitalClockKind.draw(env(DigitalClockKind, 160f, 160f), Unit).clocks()
        assertEquals(listOf("HH:mm", "EEEE d MMMM"), clocks.map { it.pattern })
    }

    @Test
    fun `l'horloge numerique respecte ses reglages`() {
        val root = DigitalClockKind.draw(
            env(DigitalClockKind, 160f, 160f, "h24" to "0", "seconds" to "1", "date" to "0", "align" to "start"), Unit,
        )
        assertEquals(listOf("h:mm:ss"), root.clocks().map { it.pattern })
        assertEquals(HAlign.START, assertIs<Column>(root).horizontal)
    }

    @Test
    fun `une petite horloge utilise une date courte`() {
        val clocks = DigitalClockKind.draw(env(DigitalClockKind, 110f, 110f), Unit).clocks()
        assertEquals("EEE d MMM", clocks.last().pattern)
    }

    @Test
    fun `un clic optionnel lance l'application choisie`() {
        val root = DigitalClockKind.draw(env(DigitalClockKind, 160f, 160f, "app" to "com.clock"), Unit)
        assertEquals(Click.LaunchApp("com.clock"), root.mod.click)
        assertEquals(null, DigitalClockKind.draw(env(DigitalClockKind, 160f, 160f), Unit).mod.click)
    }

    @Test
    fun `l'horloge empilee superpose heures et minutes`() {
        val clocks = StackedClockKind.draw(env(StackedClockKind, 160f, 160f), Unit).clocks()
        assertEquals(listOf("HH", "mm"), clocks.map { it.pattern })
        assertEquals(accent, clocks.first().color)
        assertTrue(clocks.all { it.bold })
    }

    @Test
    fun `l'horloge analogique choisit son cadran`() {
        val node = AnalogClockKind.draw(env(AnalogClockKind, 160f, 160f, "face" to "CLASSIC"), Unit)
        val clock = node.walk().filterIsInstance<AnalogClock>().single()
        assertEquals(AnalogFace.CLASSIC, clock.face)
        assertTrue(clock.sizeDp in 40f..160f)
        assertTrue(AnalogClockKind.draw(env(AnalogClockKind, 160f, 160f, "face" to "inconnu"), Unit)
            .walk().filterIsInstance<AnalogClock>().single().face == AnalogFace.LIGHT)
    }

    @Test
    fun `l'horloge mondiale affiche autant de villes que la hauteur le permet`() {
        val tall = WorldClockKind.draw(env(WorldClockKind, 320f, 160f), Unit).clocks()
        assertEquals(listOf("Europe/Paris", "America/New_York", "Asia/Tokyo"), tall.map { it.timeZone })
        val bar = WorldClockKind.draw(env(WorldClockKind, 320f, 72f), Unit).clocks()
        assertEquals(1, bar.size)
    }

    @Test
    fun `l'horloge mondiale nomme les villes en francais`() {
        val node = WorldClockKind.draw(env(WorldClockKind, 320f, 160f, "zone2" to "America/Sao_Paulo"), Unit)
        assertTrue("São Paulo" in node.texts())
        assertTrue("Paris" in node.texts())
    }

    // ---- Calendriers

    @Test
    fun `le calendrier du mois affiche tous les jours et met aujourd'hui en valeur`() {
        val node = MonthCalendarKind.draw(env(MonthCalendarKind, 320f, 320f), Unit)
        val days = node.texts().filter { it.toIntOrNull() != null }
        assertEquals((1..30).map { it.toString() }, days.sortedBy { it.toInt() })

        val highlighted = node.walk().filterIsInstance<Box>().filter { it.mod.background == accent }.toList()
        assertEquals(1, highlighted.size)
        assertEquals("4", assertIs<Text>(highlighted.single().children.single()).text)
    }

    @Test
    fun `le calendrier du mois a le bon titre et les bonnes initiales`() {
        val texts = MonthCalendarKind.draw(env(MonthCalendarKind, 320f, 320f), Unit).texts()
        assertTrue("Novembre 2026" in texts, texts.toString())
        assertEquals(listOf("L", "M", "M", "J", "V", "S", "D"), texts.filter { it.length == 1 && it.first().isLetter() })
    }

    @Test
    fun `le calendrier commence le dimanche si demande`() {
        val node = MonthCalendarKind.draw(env(MonthCalendarKind, 320f, 320f, "monday" to "0"), Unit)
        val letters = node.texts().filter { it.length == 1 && it.first().isLetter() }
        assertEquals("D", letters.first())
        val rows = node.walk().filterIsInstance<Row>().count { it.children.size == 7 }
        assertEquals(1 + 5, rows) // ligne des initiales + 5 semaines
    }

    @Test
    fun `la semaine affiche sept jours dont le jour courant en valeur`() {
        val node = WeekStripKind.draw(env(WeekStripKind, 320f, 72f), Unit)
        assertEquals(listOf("2", "3", "4", "5", "6", "7", "8"), node.texts().filter { it.toIntOrNull() != null })
        assertEquals(1, node.walk().filterIsInstance<Box>().count { it.mod.background == accent })
    }

    @Test
    fun `la grande date affiche le jour en toutes lettres`() {
        val texts = BigDateKind.draw(env(BigDateKind, 160f, 160f), Unit).texts()
        assertEquals(listOf("MERCREDI", "4", "Novembre 2026"), texts)
    }

    @Test
    fun `le compte a rebours calcule les jours restants`() {
        val node = CountdownKind.draw(env(CountdownKind, 160f, 160f, "date" to "2026-12-01", "title" to "Noël"), Unit)
        assertEquals(listOf("Noël", "27", "s${R.string.countdown_days_left}"), node.texts())
    }

    @Test
    fun `le compte a rebours gere le singulier, aujourd'hui et le passe`() {
        fun texts(date: String) = CountdownKind.draw(env(CountdownKind, 160f, 160f, "date" to date), Unit).texts()
        assertTrue("s${R.string.countdown_day_left}" in texts("2026-11-05"))
        assertTrue("s${R.string.countdown_today}" in texts("2026-11-04"))
        assertTrue("s${R.string.countdown_days_ago}" in texts("2026-10-20"))
        assertTrue("s${R.string.countdown_day_ago}" in texts("2026-11-03"))
        assertTrue("s${R.string.countdown_pick_date}" in texts(""))
    }

    @Test
    fun `le compte a rebours exige une date valide`() {
        assertFalse(CountdownKind.isComplete(Params()))
        assertFalse(CountdownKind.isComplete(Params(mapOf("date" to "demain"))))
        assertTrue(CountdownKind.isComplete(Params(mapOf("date" to "2026-12-01"))))
    }

    @Test
    fun `la progression affiche quatre barres sur un grand widget et deux sur un petit`() {
        fun percentages(h: Float) = ProgressKind.draw(env(ProgressKind, 160f, h), Unit).texts().filter { it.endsWith(" %") }
        assertEquals(4, percentages(160f).size)
        assertEquals(2, percentages(100f).size)
    }

    // ---- Applications

    @Test
    fun `l'icone exige une application et ouvre la configuration sans elle`() {
        assertFalse(AppIconKind.isComplete(Params()))
        assertEquals(Click.Configure, AppIconKind.draw(env(AppIconKind, 72f, 72f), Unit).mod.click)
    }

    @Test
    fun `l'icone lance l'application et demande son icone`() {
        val node = AppIconKind.draw(env(AppIconKind, 160f, 160f, "app" to "com.chrome", "app_label" to "Chrome"), Unit)
        assertEquals(Click.LaunchApp("com.chrome"), node.mod.click)
        assertTrue(ImgSource.AppIcon("com.chrome") in node.imageRequests())
        assertEquals(listOf("Chrome"), node.texts())
    }

    @Test
    fun `l'icone masque son nom sur un widget trop bas`() {
        val node = AppIconKind.draw(env(AppIconKind, 72f, 60f, "app" to "com.chrome", "app_label" to "Chrome"), Unit)
        assertTrue(node.texts().isEmpty())
    }

    @Test
    fun `la pilule affiche le libelle choisi ou le nom de l'application`() {
        val custom = AppPillKind.draw(env(AppPillKind, 320f, 72f, "app" to "a.b", "text" to "Enregistrer"), Unit)
        assertEquals(listOf("Enregistrer"), custom.texts())
        val byDefault = AppPillKind.draw(env(AppPillKind, 320f, 72f, "app" to "a.b", "app_label" to "Dictaphone"), Unit)
        assertEquals(listOf("Dictaphone"), byDefault.texts())
        assertTrue(byDefault.walk().filterIsInstance<Box>().any { it.mod.background != null })
    }

    @Test
    fun `la barre de recherche affiche son texte d'invite`() {
        val byDefault = SearchBarKind.draw(env(SearchBarKind, 320f, 72f, "app" to "a.b"), Unit)
        assertEquals(listOf("s${R.string.search_hint_default}"), byDefault.texts())
        val custom = SearchBarKind.draw(env(SearchBarKind, 320f, 72f, "app" to "a.b", "hint" to "Chercher…"), Unit)
        assertEquals(listOf("Chercher…"), custom.texts())
    }

    @Test
    fun `le dossier est plafonne a huit applications en quatre colonnes`() {
        val many = (1..12).joinToString(",") { "app.n$it" }
        val node = AppFolderKind.draw(env(AppFolderKind, 320f, 160f, "apps" to many), Unit)
        assertEquals(8, node.imageRequests().size)
        val rows = node.walk().filterIsInstance<Row>().toList()
        assertEquals(listOf(4, 4), rows.map { it.children.size })
    }

    @Test
    fun `un dossier etroit passe a trois colonnes et chaque icone a son clic`() {
        val node = AppFolderKind.draw(env(AppFolderKind, 160f, 160f, "apps" to "a.a,b.b,c.c,d.d"), Unit)
        assertEquals(listOf(3, 1), node.walk().filterIsInstance<Row>().map { it.children.size }.toList())
        val clicks = node.walk().filterIsInstance<Box>().mapNotNull { it.mod.click }.toList()
        assertEquals(listOf("a.a", "b.b", "c.c", "d.d").map { Click.LaunchApp(it) }, clicks)
    }

    @Test
    fun `le dossier vide propose d'ajouter des applications et n'est pas complet`() {
        assertFalse(AppFolderKind.isComplete(Params()))
        val node = AppFolderKind.draw(env(AppFolderKind, 160f, 160f), Unit)
        assertEquals("s${R.string.folder_empty}", node.texts().first())
    }

    @Test
    fun `le titre du dossier n'apparait que si le widget est assez haut`() {
        val tall = AppFolderKind.draw(env(AppFolderKind, 160f, 160f, "apps" to "a.a", "title" to "Messagerie"), Unit)
        assertTrue("Messagerie" in tall.texts())
        val short = AppFolderKind.draw(env(AppFolderKind, 320f, 72f, "apps" to "a.a", "title" to "Messagerie"), Unit)
        assertFalse("Messagerie" in short.texts())
    }

    @Test
    fun `tous les types du catalogue de test ont un identifiant unique et des valeurs par defaut coherentes`() {
        val kinds = listOf(
            DigitalClockKind, StackedClockKind, AnalogClockKind, WorldClockKind, MonthCalendarKind, WeekStripKind,
            BigDateKind, CountdownKind, ProgressKind, AppIconKind, AppPillKind, AppFolderKind, SearchBarKind,
        )
        assertEquals(kinds.size, kinds.map { it.id }.toSet().size)
        kinds.forEach { kind ->
            // Chaque champ déclaré a une clé unique, et un type de widget produit toujours un arbre sans lever d'exception.
            assertEquals(kind.fields.size, kind.fields.map { it.key }.toSet().size, kind.id)
            val node = kind.draw(env(kind, 320f, 160f))
            assertNotNull(node)
        }
    }
}
