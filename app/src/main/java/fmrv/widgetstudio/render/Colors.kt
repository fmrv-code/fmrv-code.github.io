package fmrv.widgetstudio.render

import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetStyle
import kotlin.math.roundToInt

/** Couleurs ARGB : fond d'un widget, transparence, contraste. Pur, partagé par les deux moteurs de rendu. */
object Colors {

    /** Part de l'opacité conservée à l'extrémité d'un fond « verre » : donne l'effet de profondeur. */
    private const val GLASS_END_ALPHA = 0.45f

    /** Couleurs de début et de fin du fond, opacité du widget appliquée. */
    fun background(style: WidgetStyle): Pair<Int, Int> = when (style.background) {
        BackgroundStyle.SOLID -> withOpacity(style.colorStart, style.opacity).let { it to it }
        BackgroundStyle.GRADIENT -> withOpacity(style.colorStart, style.opacity) to withOpacity(style.colorEnd, style.opacity)
        BackgroundStyle.GLASS -> withOpacity(style.colorStart, style.opacity) to
            withOpacity(style.colorStart, style.opacity * GLASS_END_ALPHA)
    }

    /** Multiplie l'alpha déjà présent dans [argb] par [opacity] (0..1). */
    fun withOpacity(argb: Int, opacity: Float): Int {
        val alpha = ((argb ushr 24) * opacity.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        return (argb and 0x00FFFFFF) or (alpha shl 24)
    }

    /** Couleur de texte lisible (noir ou blanc) sur un fond [argb]. */
    fun onColor(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return if (luminance > 0.6) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
    }
}
