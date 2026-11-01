package fmrv.widgetstudio.widget

import fmrv.widgetstudio.domain.model.WidgetShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetStyleTest {

    @Test
    fun `les rayons croissent du carre a la capsule`() {
        val square = WidgetStyle.cornerRadiusDp(WidgetShape.SQUARE)
        val rounded = WidgetStyle.cornerRadiusDp(WidgetShape.ROUNDED)
        val pill = WidgetStyle.cornerRadiusDp(WidgetShape.PILL)
        assertTrue(square < rounded && rounded < pill)
    }

    @Test
    fun `le nom est masque sur un widget trop bas ou desactive`() {
        assertFalse(WidgetStyle.isLabelVisible(showLabel = true, heightDp = 60f))
        assertTrue(WidgetStyle.isLabelVisible(showLabel = true, heightDp = 110f))
        assertFalse(WidgetStyle.isLabelVisible(showLabel = false, heightDp = 200f))
    }

    @Test
    fun `la taille de l'icone reste bornee`() {
        assertEquals(28f, WidgetStyle.iconSizeDp(10f, 10f, labelVisible = false))
        assertEquals(72f, WidgetStyle.iconSizeDp(1000f, 1000f, labelVisible = true))
    }

    @Test
    fun `l'icone est plus grande quand le nom est masque`() {
        assertTrue(
            WidgetStyle.iconSizeDp(100f, 100f, labelVisible = false) >
                WidgetStyle.iconSizeDp(100f, 100f, labelVisible = true),
        )
    }

    @Test
    fun `la taille de l'heure reste bornee et diminue quand la date est affichee`() {
        assertEquals(18f, WidgetStyle.timeSizeSp(10f, 10f, showDate = false))
        assertEquals(64f, WidgetStyle.timeSizeSp(2000f, 2000f, showDate = false))
        assertTrue(
            WidgetStyle.timeSizeSp(300f, 100f, showDate = true) <=
                WidgetStyle.timeSizeSp(300f, 100f, showDate = false),
        )
    }

    @Test
    fun `la taille de la date suit celle de l'heure dans ses bornes`() {
        assertEquals(10f, WidgetStyle.dateSizeSp(18f))
        assertEquals(18f, WidgetStyle.dateSizeSp(64f))
    }

    @Test
    fun `les motifs d'heure et de date dependent du format et de la largeur`() {
        assertEquals("HH:mm", WidgetStyle.timePattern(use24Hour = true))
        assertEquals("h:mm", WidgetStyle.timePattern(use24Hour = false))
        assertEquals("EEE d MMM", WidgetStyle.datePattern(100f))
        assertEquals("EEEE d MMMM", WidgetStyle.datePattern(300f))
    }

    @Test
    fun `la pastille d'une pilule reste bornee`() {
        assertEquals(28f, WidgetStyle.pillCircleDp(20f))
        assertEquals(64f, WidgetStyle.pillCircleDp(400f))
        assertEquals(56f, WidgetStyle.pillCircleDp(72f))
    }

    @Test
    fun `la grille compte trois ou quatre colonnes selon la largeur`() {
        assertEquals(3, WidgetStyle.gridColumns(160f))
        assertEquals(4, WidgetStyle.gridColumns(320f))
    }

    @Test
    fun `le nombre de lignes de la grille est arrondi au-dessus`() {
        assertEquals(0, WidgetStyle.gridRows(0, 4))
        assertEquals(1, WidgetStyle.gridRows(4, 4))
        assertEquals(2, WidgetStyle.gridRows(5, 4))
        assertEquals(3, WidgetStyle.gridRows(7, 3))
    }

    @Test
    fun `les icones du dossier restent dans leurs bornes`() {
        assertEquals(24f, WidgetStyle.gridIconDp(40f, 40f, 4, 2, labelVisible = false))
        assertEquals(52f, WidgetStyle.gridIconDp(1000f, 1000f, 4, 2, labelVisible = false))
    }

    @Test
    fun `le nom du dossier n'est affiche que sur un widget assez haut`() {
        assertTrue(WidgetStyle.showGridLabel(true, 160f))
        assertFalse(WidgetStyle.showGridLabel(true, 100f))
        assertFalse(WidgetStyle.showGridLabel(false, 300f))
    }
}
