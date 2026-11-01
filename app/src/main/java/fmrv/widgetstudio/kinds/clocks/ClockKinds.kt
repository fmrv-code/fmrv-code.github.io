package fmrv.widgetstudio.kinds.clocks

import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.StylePresets
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.kinds.ChoiceOption
import fmrv.widgetstudio.kinds.Fit
import fmrv.widgetstudio.kinds.LoadEnv
import fmrv.widgetstudio.kinds.ParamField
import fmrv.widgetstudio.kinds.RenderEnv
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.kinds.hAlignOf
import fmrv.widgetstudio.kinds.textAlignOf
import fmrv.widgetstudio.render.AnalogClock
import fmrv.widgetstudio.render.AnalogFace
import fmrv.widgetstudio.render.Click
import fmrv.widgetstudio.render.ClockText
import fmrv.widgetstudio.render.Colors
import fmrv.widgetstudio.render.HAlign
import fmrv.widgetstudio.render.Mod
import fmrv.widgetstudio.render.Node
import fmrv.widgetstudio.render.Pad
import fmrv.widgetstudio.render.Text
import fmrv.widgetstudio.render.TextAlign
import fmrv.widgetstudio.render.TextWeight
import fmrv.widgetstudio.render.VAlign
import fmrv.widgetstudio.render.box
import fmrv.widgetstudio.render.column
import fmrv.widgetstudio.render.row

private val AlignOptions = listOf(
    ChoiceOption("start", R.string.align_start),
    ChoiceOption("center", R.string.align_center),
    ChoiceOption("end", R.string.align_end),
)

/** Clic optionnel : ouvre l'application choisie dans le réglage "app". */
private fun appClick(env: RenderEnv): Click? =
    env.params.string("app").takeIf { it.isNotEmpty() }?.let { Click.LaunchApp(it) }

private fun datePattern(widthDp: Float) = if (widthDp >= 150f) "EEEE d MMMM" else "EEE d MMM"

/** Heure et date en texte, tailles adaptées à la place disponible. */
object DigitalClockKind : WidgetKind<Unit> {
    override val id = "clock.digital"
    override val category = Category.CLOCKS
    override val title = R.string.kind_clock_digital
    override val description = R.string.kind_clock_digital_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.BlueNight.style
    override val defaultParams = mapOf("h24" to "1", "date" to "1", "seconds" to "0", "align" to "center")
    override val fields = listOf(
        ParamField.Toggle("h24", R.string.option_24h),
        ParamField.Toggle("date", R.string.option_show_date),
        ParamField.Toggle("seconds", R.string.option_seconds),
        ParamField.Choice("align", R.string.field_align, AlignOptions),
        ParamField.AppPick("app", R.string.section_app_optional),
    )

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val p = env.params
        val h24 = p.bool("h24", true)
        val seconds = p.bool("seconds", false)
        val showDate = p.bool("date", true)
        val align = p.string("align", "center")
        val w = env.widthDp - 16f
        val h = env.heightDp - 16f

        val timeSp = Fit.sp(w, h, if (seconds) 0.2f else 0.26f, if (showDate) 0.42f else 0.55f, 18f, 80f)
        val pattern = (if (h24) "HH:mm" else "h:mm") + if (seconds) ":ss" else ""

        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.all(8f), click = appClick(env)),
            h = hAlignOf(align),
            v = VAlign.CENTER,
        ) {
            +ClockText(pattern, timeSp, env.text, env.font, align = textAlignOf(align), mod = Mod(fillWidth = true))
            if (showDate) {
                +ClockText(
                    datePattern(env.widthDp), (timeSp * 0.36f).coerceIn(10f, 18f), Colors.withOpacity(env.text, 0.8f),
                    env.font, align = textAlignOf(align), mod = Mod(fillWidth = true),
                )
            }
        }
    }
}

/** Heures et minutes empilées en très grand, l'heure dans la couleur d'accent. */
object StackedClockKind : WidgetKind<Unit> {
    override val id = "clock.stack"
    override val category = Category.CLOCKS
    override val title = R.string.kind_clock_stack
    override val description = R.string.kind_clock_stack_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.Black.style.copy(accentColor = 0xFFFF6B4A.toInt())
    override val defaultParams = mapOf("h24" to "1", "align" to "center")
    override val fields = listOf(
        ParamField.Toggle("h24", R.string.option_24h),
        ParamField.Choice("align", R.string.field_align, AlignOptions),
        ParamField.AppPick("app", R.string.section_app_optional),
    )

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val align = env.params.string("align", "center")
        val sp = Fit.sp(env.widthDp - 16f, env.heightDp - 16f, 0.42f, 0.34f, 28f, 120f)
        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.all(8f), click = appClick(env)),
            h = hAlignOf(align),
            v = VAlign.CENTER,
        ) {
            +ClockText(if (env.params.bool("h24", true)) "HH" else "h", sp, env.accent, env.font, bold = true, align = textAlignOf(align), mod = Mod(fillWidth = true))
            +ClockText("mm", sp, env.text, env.font, bold = true, align = textAlignOf(align), mod = Mod(fillWidth = true))
        }
    }
}

/** Horloge à aiguilles en quatre visuels. */
object AnalogClockKind : WidgetKind<Unit> {
    override val id = "clock.analog"
    override val category = Category.CLOCKS
    override val title = R.string.kind_clock_analog
    override val description = R.string.kind_clock_analog_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.Transparent.style
    override val defaultParams = mapOf("face" to "LIGHT", "date" to "0")
    override val fields = listOf(
        ParamField.Choice(
            "face", R.string.field_face,
            listOf(
                ChoiceOption("LIGHT", R.string.face_light),
                ChoiceOption("DARK", R.string.face_dark),
                ChoiceOption("CLASSIC", R.string.face_classic),
                ChoiceOption("BOLD", R.string.face_bold),
            ),
        ),
        ParamField.Toggle("date", R.string.option_show_date),
        ParamField.AppPick("app", R.string.section_app_optional),
    )

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val showDate = env.params.bool("date", false)
        val face = AnalogFace.entries.firstOrNull { it.name == env.params.string("face", "LIGHT") } ?: AnalogFace.LIGHT
        val size = (minOf(env.widthDp - 16f, env.heightDp - 16f - if (showDate) 26f else 0f)).coerceAtLeast(40f)
        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.all(8f), click = appClick(env)),
            h = HAlign.CENTER,
            v = VAlign.CENTER,
        ) {
            +AnalogClock(face, size)
            if (showDate) {
                +ClockText("EEE d MMM", 13f, env.text, env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true, pad = Pad(top = 4f)))
            }
        }
    }
}

/** Jusqu'à trois villes avec leur heure locale. */
object WorldClockKind : WidgetKind<Unit> {
    override val id = "clock.world"
    override val category = Category.CLOCKS
    override val title = R.string.kind_clock_world
    override val description = R.string.kind_clock_world_desc
    override val defaultSize = WidgetSizeOption.WIDE
    override val defaultStyle = StylePresets.GlassDark.style
    override val defaultParams = mapOf(
        "zone1" to "Europe/Paris",
        "zone2" to "America/New_York",
        "zone3" to "Asia/Tokyo",
        "h24" to "1",
    )
    override val fields = listOf(
        ParamField.Choice("zone1", R.string.field_city_1, Zones.options),
        ParamField.Choice("zone2", R.string.field_city_2, Zones.options),
        ParamField.Choice("zone3", R.string.field_city_3, Zones.options),
        ParamField.Toggle("h24", R.string.option_24h),
    )

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val rows = ((env.heightDp - 16f) / ROW_DP).toInt().coerceIn(1, 3)
        val zones = listOf("zone1" to "Europe/Paris", "zone2" to "America/New_York", "zone3" to "Asia/Tokyo")
            .take(rows)
            .map { (key, default) -> env.params.string(key, default) }
        val pattern = if (env.params.bool("h24", true)) "HH:mm" else "h:mm a"
        val divider = Colors.withOpacity(env.text, 0.15f)

        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.sym(horizontal = 14f, vertical = 8f)),
            v = VAlign.CENTER,
        ) {
            zones.forEachIndexed { index, zone ->
                if (index > 0) {
                    +box(Mod(fillWidth = true, height = 1f, background = divider)) {}
                }
                +row(Mod(fillWidth = true, height = ROW_DP), v = VAlign.CENTER) {
                    +Text(
                        Zones.label(zone), 14f, env.text, TextWeight.MEDIUM, env.font,
                        mod = Mod(weight = 1f),
                    )
                    +ClockText(pattern, 24f, env.text, env.font, bold = true, align = TextAlign.END, timeZone = zone)
                }
            }
        }
    }

    private const val ROW_DP = 44f
}
