package fmrv.widgetstudio.widget

import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WidgetBackgroundTest {

    private val base = WidgetConfig(
        appWidgetId = 1,
        backgroundColor = 0xFF102030.toInt(),
        backgroundEnd = 0xFFA0B0C0.toInt(),
    )

    private fun alpha(argb: Int) = argb ushr 24

    @Test
    fun `un fond uni a la meme couleur au debut et a la fin`() {
        val (start, end) = WidgetBackground.colors(base.copy(background = BackgroundStyle.SOLID, opacity = 1f))
        assertEquals(0xFF102030.toInt(), start)
        assertEquals(start, end)
    }

    @Test
    fun `un degrade utilise les deux couleurs`() {
        val (start, end) = WidgetBackground.colors(base.copy(background = BackgroundStyle.GRADIENT))
        assertEquals(0xFF102030.toInt(), start)
        assertEquals(0xFFA0B0C0.toInt(), end)
    }

    @Test
    fun `le verre perd de l'opacite vers la fin avec une seule couleur`() {
        val (start, end) = WidgetBackground.colors(base.copy(background = BackgroundStyle.GLASS, opacity = 0.6f))
        assertEquals(start and 0xFFFFFF, end and 0xFFFFFF)
        assertTrue(alpha(end) < alpha(start), "la fin doit être plus transparente")
        assertEquals(153, alpha(start)) // 255 × 0,6
    }

    @Test
    fun `l'opacite du widget multiplie l'alpha existant de la couleur`() {
        assertEquals(128, alpha(WidgetBackground.withOpacity(0xFFFFFFFF.toInt(), 0.5f)))
        assertEquals(64, alpha(WidgetBackground.withOpacity(0x80FFFFFF.toInt(), 0.5f)))
    }

    @Test
    fun `l'opacite est bornee et les composantes de couleur conservees`() {
        assertEquals(255, alpha(WidgetBackground.withOpacity(0xFF000000.toInt(), 5f)))
        assertEquals(0, alpha(WidgetBackground.withOpacity(0xFF000000.toInt(), -1f)))
        assertEquals(0x123456, WidgetBackground.withOpacity(0xFF123456.toInt(), 0.3f) and 0xFFFFFF)
    }

    @Test
    fun `un fond transparent le reste`() {
        val (start, _) = WidgetBackground.colors(base.copy(backgroundColor = 0x00000000, opacity = 1f))
        assertEquals(0, alpha(start))
    }
}
