package fmrv.widgetstudio.render

import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorsTest {

    private val base = WidgetStyle(colorStart = 0xFF102030.toInt(), colorEnd = 0xFFA0B0C0.toInt())

    private fun alpha(argb: Int) = argb ushr 24

    @Test
    fun `un fond uni a la meme couleur au debut et a la fin`() {
        val (start, end) = Colors.background(base.copy(background = BackgroundStyle.SOLID))
        assertEquals(0xFF102030.toInt(), start)
        assertEquals(start, end)
    }

    @Test
    fun `un degrade utilise les deux couleurs`() {
        val (start, end) = Colors.background(base.copy(background = BackgroundStyle.GRADIENT))
        assertEquals(0xFF102030.toInt(), start)
        assertEquals(0xFFA0B0C0.toInt(), end)
    }

    @Test
    fun `le verre perd de l'opacite vers la fin avec une seule couleur`() {
        val (start, end) = Colors.background(base.copy(background = BackgroundStyle.GLASS, opacity = 0.6f))
        assertEquals(start and 0xFFFFFF, end and 0xFFFFFF)
        assertTrue(alpha(end) < alpha(start))
        assertEquals(153, alpha(start))
    }

    @Test
    fun `l'opacite multiplie l'alpha existant et reste bornee`() {
        assertEquals(128, alpha(Colors.withOpacity(0xFFFFFFFF.toInt(), 0.5f)))
        assertEquals(64, alpha(Colors.withOpacity(0x80FFFFFF.toInt(), 0.5f)))
        assertEquals(255, alpha(Colors.withOpacity(0xFF000000.toInt(), 5f)))
        assertEquals(0, alpha(Colors.withOpacity(0xFF000000.toInt(), -1f)))
        assertEquals(0x123456, Colors.withOpacity(0xFF123456.toInt(), 0.3f) and 0xFFFFFF)
    }

    @Test
    fun `un fond transparent le reste`() {
        val (start, _) = Colors.background(base.copy(colorStart = 0x00000000))
        assertEquals(0, alpha(start))
    }

    @Test
    fun `le texte sur un fond clair est sombre et inversement`() {
        assertEquals(0xFF111111.toInt(), Colors.onColor(0xFFFFFFFF.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), Colors.onColor(0xFF000000.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), Colors.onColor(0xFF3E91FF.toInt()))
    }
}
