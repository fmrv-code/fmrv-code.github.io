package fmrv.widgetstudio.widget

import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetConfig
import kotlin.math.roundToInt

/** Couleurs (ARGB, opacité du widget appliquée) du début et de la fin du fond. Pure, partagée avec l'aperçu. */
object WidgetBackground {

    /** Part de l'opacité conservée à l'extrémité d'un fond « verre » : donne l'effet de profondeur. */
    private const val GLASS_END_ALPHA = 0.45f

    fun colors(config: WidgetConfig): Pair<Int, Int> = when (config.background) {
        BackgroundStyle.SOLID -> {
            val c = withOpacity(config.backgroundColor, config.opacity)
            c to c
        }
        BackgroundStyle.GRADIENT -> withOpacity(config.backgroundColor, config.opacity) to
            withOpacity(config.backgroundEnd, config.opacity)
        BackgroundStyle.GLASS -> withOpacity(config.backgroundColor, config.opacity) to
            withOpacity(config.backgroundColor, config.opacity * GLASS_END_ALPHA)
    }

    /** Multiplie l'alpha déjà présent dans [argb] par [opacity] (0..1). */
    fun withOpacity(argb: Int, opacity: Float): Int {
        val alpha = ((argb ushr 24) * opacity.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        return (argb and 0x00FFFFFF) or (alpha shl 24)
    }
}
