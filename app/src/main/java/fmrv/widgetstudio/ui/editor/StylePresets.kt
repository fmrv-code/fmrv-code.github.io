package fmrv.widgetstudio.ui.editor

import androidx.annotation.StringRes
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetConfig

/** Style complet (fond, couleurs, transparence) appliqué en un geste, dans l'esprit des widgets One UI. */
data class StylePreset(
    val id: String,
    @StringRes val name: Int,
    val background: BackgroundStyle,
    val start: Int,
    val end: Int,
    val text: Int,
    val accent: Int,
    val opacity: Float,
) {
    fun applyTo(config: WidgetConfig): WidgetConfig = config.copy(
        background = background,
        backgroundColor = start,
        backgroundEnd = end,
        textColor = text,
        accentColor = accent,
        opacity = opacity,
    )

    fun isAppliedTo(config: WidgetConfig): Boolean =
        config.background == background &&
            config.backgroundColor == start &&
            config.backgroundEnd == end &&
            config.textColor == text &&
            config.accentColor == accent &&
            config.opacity == opacity
}

private fun argb(value: Long): Int = value.toInt()

object StylePresets {
    val BlueNight = StylePreset("blue_night", R.string.style_blue_night, BackgroundStyle.GRADIENT, argb(0xFF1B3A7A), argb(0xFF2F6BB8), argb(0xFFFFFFFF), argb(0xFF7CC4FF), 1f)
    val Coral = StylePreset("coral", R.string.style_coral, BackgroundStyle.GRADIENT, argb(0xFFF59E6B), argb(0xFFE8643A), argb(0xFFFFFFFF), argb(0xFFFFE0D0), 1f)
    val GlassLight = StylePreset("glass_light", R.string.style_glass_light, BackgroundStyle.GLASS, argb(0xFFFFFFFF), argb(0xFFFFFFFF), argb(0xFF1C1C1E), argb(0xFF3E91FF), 0.6f)
    val GlassDark = StylePreset("glass_dark", R.string.style_glass_dark, BackgroundStyle.GLASS, argb(0xFF101018), argb(0xFF101018), argb(0xFFFFFFFF), argb(0xFF7CC4FF), 0.6f)
    val Lavender = StylePreset("lavender", R.string.style_lavender, BackgroundStyle.GRADIENT, argb(0xFF6D5CFF), argb(0xFFB07CFF), argb(0xFFFFFFFF), argb(0xFFE5DAFF), 1f)
    val Mint = StylePreset("mint", R.string.style_mint, BackgroundStyle.GRADIENT, argb(0xFF12B5A0), argb(0xFF4DD58E), argb(0xFF05342E), argb(0xFFFFFFFF), 1f)
    val Black = StylePreset("black", R.string.style_black, BackgroundStyle.SOLID, argb(0xFF0B0B0D), argb(0xFF0B0B0D), argb(0xFFFFFFFF), argb(0xFF3E91FF), 1f)
    val White = StylePreset("white", R.string.style_white, BackgroundStyle.SOLID, argb(0xFFFFFFFF), argb(0xFFFFFFFF), argb(0xFF1C1C1E), argb(0xFF3E91FF), 1f)
    val OneUiBlue = StylePreset("one_ui_blue", R.string.style_one_ui_blue, BackgroundStyle.SOLID, argb(0xFF3E91FF), argb(0xFF3E91FF), argb(0xFFFFFFFF), argb(0xFFFFFFFF), 1f)
    val Transparent = StylePreset("transparent", R.string.style_transparent, BackgroundStyle.SOLID, argb(0x00000000), argb(0x00000000), argb(0xFFFFFFFF), argb(0xFF3E91FF), 1f)

    val all = listOf(BlueNight, Coral, GlassLight, GlassDark, Lavender, Mint, Black, White, OneUiBlue, Transparent)
}
