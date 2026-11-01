package fmrv.widgetstudio.domain.model

import androidx.annotation.StringRes
import fmrv.widgetstudio.R

/** Style complet appliqué en un geste, dans l'esprit des widgets One UI. */
data class StylePreset(
    val id: String,
    @StringRes val name: Int,
    val style: WidgetStyle,
) {
    fun isAppliedTo(other: WidgetStyle): Boolean =
        other.background == style.background &&
            other.colorStart == style.colorStart &&
            other.colorEnd == style.colorEnd &&
            other.textColor == style.textColor &&
            other.accentColor == style.accentColor &&
            other.opacity == style.opacity

    /** Applique les couleurs et le fond en conservant la forme et la police choisies. */
    fun applyTo(other: WidgetStyle): WidgetStyle = style.copy(shape = other.shape, font = other.font)
}

private fun argb(value: Long): Int = value.toInt()

private fun preset(
    id: String,
    @StringRes name: Int,
    background: BackgroundStyle,
    start: Long,
    end: Long,
    text: Long,
    accent: Long,
    opacity: Float = 1f,
) = StylePreset(
    id, name,
    WidgetStyle(
        background = background,
        colorStart = argb(start),
        colorEnd = argb(end),
        textColor = argb(text),
        accentColor = argb(accent),
        opacity = opacity,
    ),
)

object StylePresets {
    val BlueNight = preset("blue_night", R.string.style_blue_night, BackgroundStyle.GRADIENT, 0xFF1B3A7A, 0xFF2F6BB8, 0xFFFFFFFF, 0xFF7CC4FF)
    val Coral = preset("coral", R.string.style_coral, BackgroundStyle.GRADIENT, 0xFFF59E6B, 0xFFE8643A, 0xFFFFFFFF, 0xFFFFFFFF)
    val GlassLight = preset("glass_light", R.string.style_glass_light, BackgroundStyle.GLASS, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF1C1C1E, 0xFF3E91FF, 0.6f)
    val GlassDark = preset("glass_dark", R.string.style_glass_dark, BackgroundStyle.GLASS, 0xFF101018, 0xFF101018, 0xFFFFFFFF, 0xFF7CC4FF, 0.6f)
    val Lavender = preset("lavender", R.string.style_lavender, BackgroundStyle.GRADIENT, 0xFF6D5CFF, 0xFFB07CFF, 0xFFFFFFFF, 0xFFFFFFFF)
    val Mint = preset("mint", R.string.style_mint, BackgroundStyle.GRADIENT, 0xFF12B5A0, 0xFF4DD58E, 0xFF05342E, 0xFFFFFFFF)
    val Black = preset("black", R.string.style_black, BackgroundStyle.SOLID, 0xFF0B0B0D, 0xFF0B0B0D, 0xFFFFFFFF, 0xFF3E91FF)
    val White = preset("white", R.string.style_white, BackgroundStyle.SOLID, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF1C1C1E, 0xFF3E91FF)
    val OneUiBlue = preset("one_ui_blue", R.string.style_one_ui_blue, BackgroundStyle.SOLID, 0xFF3E91FF, 0xFF3E91FF, 0xFFFFFFFF, 0xFFFFFFFF)
    val Transparent = preset("transparent", R.string.style_transparent, BackgroundStyle.SOLID, 0x00000000, 0x00000000, 0xFFFFFFFF, 0xFF3E91FF)

    val all = listOf(BlueNight, Coral, GlassLight, GlassDark, Lavender, Mint, Black, White, OneUiBlue, Transparent)
}
