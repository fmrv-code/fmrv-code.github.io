package fmrv.widgetstudio.kinds.calendar

import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.StylePresets
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.kinds.Fit
import fmrv.widgetstudio.kinds.LoadEnv
import fmrv.widgetstudio.kinds.ParamField
import fmrv.widgetstudio.kinds.RenderEnv
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.render.Click
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
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private fun Locale.cap(text: String): String = text.replaceFirstChar { it.titlecase(this) }

private fun calendarClick(env: RenderEnv): Click =
    Click.OpenUri("content://com.android.calendar/time/${env.now.toInstant().toEpochMilli()}")

private fun weekStart(env: RenderEnv): DayOfWeek =
    if (env.params.bool("monday", true)) DayOfWeek.MONDAY else DayOfWeek.SUNDAY

/** Grille du mois avec le jour courant mis en valeur. */
object MonthCalendarKind : WidgetKind<Unit> {
    override val id = "calendar.month"
    override val category = Category.CALENDAR
    override val title = R.string.kind_calendar_month
    override val description = R.string.kind_calendar_month_desc
    override val defaultSize = WidgetSizeOption.LARGE
    override val defaultStyle = StylePresets.GlassLight.style
    override val defaultParams = mapOf("monday" to "1")
    override val fields = listOf(ParamField.Toggle("monday", R.string.option_monday_first))

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val today = env.now.toLocalDate()
        val start = weekStart(env)
        val first = today.withDayOfMonth(1)
        val blanks = CalendarMath.leadingBlanks(first, start)
        val days = today.lengthOfMonth()
        val weeks = CalendarMath.weeksInGrid(blanks, days)

        val headerDp = 30f
        val dowDp = 20f
        val cell = ((env.widthDp - 20f) / 7f)
        val rowDp = minOf(cell, (env.heightDp - 20f - headerDp - dowDp) / weeks).coerceAtLeast(16f)
        val daySp = (minOf(cell, rowDp) * 0.42f).coerceIn(9f, 17f)
        val dim = Colors.withOpacity(env.text, 0.55f)

        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.all(10f)),
            h = HAlign.CENTER,
            v = VAlign.CENTER,
        ) {
            val title = env.locale.cap(today.month.getDisplayName(TextStyle.FULL, env.locale)) + " " + today.year
            +Text(
                title, (headerDp * 0.58f), env.text, TextWeight.BOLD, env.font,
                mod = Mod(fillWidth = true, height = headerDp, click = calendarClick(env)),
            )
            +row(Mod(fillWidth = true, height = dowDp)) {
                for (c in 0 until 7) {
                    val dow = DayOfWeek.of((start.value - 1 + c) % 7 + 1)
                    +box(Mod(width = cell, height = dowDp)) {
                        +Text(dow.getDisplayName(TextStyle.NARROW, env.locale).uppercase(env.locale), 11f, dim, TextWeight.MEDIUM, align = TextAlign.CENTER)
                    }
                }
            }
            for (week in 0 until weeks) {
                +row(Mod(fillWidth = true, height = rowDp)) {
                    for (c in 0 until 7) {
                        val day = CalendarMath.dayAt(week, c, blanks, days)
                        if (day == null) {
                            +box(Mod(width = cell, height = rowDp)) {}
                        } else {
                            +box(Mod(width = cell, height = rowDp)) {
                                val size = minOf(cell, rowDp)
                                if (day == today.dayOfMonth) {
                                    val circle = size * 0.9f
                                    +box(Mod(width = circle, height = circle, background = env.accent, corner = circle / 2f)) {
                                        +Text(day.toString(), daySp, Colors.onColor(env.accent), TextWeight.BOLD, env.font, align = TextAlign.CENTER)
                                    }
                                } else {
                                    +Text(day.toString(), daySp, env.text, TextWeight.NORMAL, env.font, align = TextAlign.CENTER)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Bandeau des sept jours de la semaine. */
object WeekStripKind : WidgetKind<Unit> {
    override val id = "calendar.week"
    override val category = Category.CALENDAR
    override val title = R.string.kind_calendar_week
    override val description = R.string.kind_calendar_week_desc
    override val defaultSize = WidgetSizeOption.BAR
    override val defaultStyle = StylePresets.BlueNight.style
    override val defaultParams = mapOf("monday" to "1")
    override val fields = listOf(ParamField.Toggle("monday", R.string.option_monday_first))

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val today = env.now.toLocalDate()
        val start = weekStart(env)
        val monday = today.minusDays(((today.dayOfWeek.value - start.value + 7) % 7).toLong())
        val cell = (env.widthDp - 16f) / 7f
        val daySp = (cell * 0.42f).coerceIn(11f, 20f)
        val circle = minOf(cell, env.heightDp * 0.5f) * 0.92f
        val dim = Colors.withOpacity(env.text, 0.65f)

        return row(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.sym(horizontal = 8f, vertical = 6f), click = calendarClick(env)),
            h = HAlign.CENTER,
            v = VAlign.CENTER,
        ) {
            for (i in 0 until 7) {
                val date = monday.plusDays(i.toLong())
                +column(Mod(width = cell), h = HAlign.CENTER, v = VAlign.CENTER) {
                    +Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, env.locale).take(3).uppercase(env.locale), 10f, dim, TextWeight.MEDIUM,
                        align = TextAlign.CENTER,
                    )
                    +box(
                        Mod(
                            width = circle, height = circle,
                            background = if (date == today) env.accent else null,
                            corner = circle / 2f,
                        ),
                    ) {
                        +Text(
                            date.dayOfMonth.toString(), daySp,
                            if (date == today) Colors.onColor(env.accent) else env.text,
                            if (date == today) TextWeight.BOLD else TextWeight.NORMAL,
                            env.font, align = TextAlign.CENTER,
                        )
                    }
                }
            }
        }
    }
}

/** Grande date : numéro du jour, jour de la semaine, mois. */
object BigDateKind : WidgetKind<Unit> {
    override val id = "calendar.date"
    override val category = Category.CALENDAR
    override val title = R.string.kind_calendar_date
    override val description = R.string.kind_calendar_date_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.Coral.style
    override val fields = emptyList<ParamField>()

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val d = env.now.toLocalDate()
        val numberSp = Fit.sp(env.widthDp - 16f, env.heightDp - 16f, 0.5f, 0.5f, 32f, 120f)
        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.all(10f), click = calendarClick(env)),
            h = HAlign.CENTER,
            v = VAlign.CENTER,
        ) {
            +Text(
                d.dayOfWeek.getDisplayName(TextStyle.FULL, env.locale).uppercase(env.locale), (numberSp * 0.2f).coerceIn(11f, 18f),
                env.accent, TextWeight.BOLD, env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true),
            )
            +Text(d.dayOfMonth.toString(), numberSp, env.text, TextWeight.BOLD, env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true))
            +Text(
                env.locale.cap(d.month.getDisplayName(TextStyle.FULL, env.locale)) + " " + d.year, (numberSp * 0.2f).coerceIn(11f, 18f),
                Colors.withOpacity(env.text, 0.85f), TextWeight.MEDIUM, env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true),
            )
        }
    }
}

/** Compte à rebours en jours jusqu'à une date, ou jours écoulés depuis. */
object CountdownKind : WidgetKind<Unit> {
    override val id = "calendar.countdown"
    override val category = Category.CALENDAR
    override val title = R.string.kind_countdown
    override val description = R.string.kind_countdown_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.Lavender.style
    override val defaultParams = mapOf("title" to "")
    override val fields = listOf(
        ParamField.Text("title", R.string.field_event_title, R.string.field_event_title_hint),
        ParamField.Date("date", R.string.field_date),
    )

    override fun isComplete(params: fmrv.widgetstudio.domain.model.Params) =
        CalendarMath.parseDate(params.string("date")) != null

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val target = CalendarMath.parseDate(env.params.string("date"))
        val title = env.params.string("title").ifBlank { env.name }
        val numberSp = Fit.sp(env.widthDp - 16f, env.heightDp - 16f, 0.42f, 0.4f, 28f, 96f)

        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.all(10f)),
            h = HAlign.CENTER,
            v = VAlign.CENTER,
        ) {
            +Text(title, 14f, Colors.withOpacity(env.text, 0.85f), TextWeight.MEDIUM, env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true))
            if (target == null) {
                +Text(env.string(R.string.countdown_pick_date), 13f, env.text, font = env.font, align = TextAlign.CENTER, maxLines = 2, mod = Mod(fillWidth = true))
            } else {
                val days = CalendarMath.daysUntil(env.now.toLocalDate(), target)
                if (days == 0L) {
                    +Text(env.string(R.string.countdown_today), (numberSp * 0.5f).coerceAtLeast(18f), env.accent, TextWeight.BOLD, env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true))
                } else {
                    val count = kotlin.math.abs(days)
                    +Text(count.toString(), numberSp, env.accent, TextWeight.BOLD, env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true))
                    val label = when {
                        days > 0 && count == 1L -> R.string.countdown_day_left
                        days > 0 -> R.string.countdown_days_left
                        count == 1L -> R.string.countdown_day_ago
                        else -> R.string.countdown_days_ago
                    }
                    +Text(env.string(label), 13f, env.text, font = env.font, align = TextAlign.CENTER, mod = Mod(fillWidth = true))
                }
            }
        }
    }
}

/** Avancement de la journée, de la semaine, du mois et de l'année en barres. */
object ProgressKind : WidgetKind<Unit> {
    override val id = "calendar.progress"
    override val category = Category.CALENDAR
    override val title = R.string.kind_progress
    override val description = R.string.kind_progress_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.Mint.style
    override val fields = emptyList<ParamField>()

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val p = CalendarMath.progress(env.now)
        val all = listOf(
            R.string.progress_year to p.year,
            R.string.progress_month to p.month,
            R.string.progress_week to p.week,
            R.string.progress_day to p.day,
        )
        val rows = ((env.heightDp - 16f) / ROW_DP).toInt().coerceIn(1, 4)
        val shown = if (rows >= 4) all else all.filter { it.first == R.string.progress_year || it.first == R.string.progress_day }.take(rows)
        val labelW = 52f
        val pctW = 36f
        val trackW = (env.widthDp - 28f - labelW - pctW - 12f).coerceAtLeast(30f)
        val track = Colors.withOpacity(env.text, 0.18f)

        return column(
            mod = Mod(fillWidth = true, fillHeight = true, pad = Pad.sym(horizontal = 14f, vertical = 8f)),
            v = VAlign.CENTER,
        ) {
            shown.forEach { (label, fraction) ->
                +row(Mod(fillWidth = true, height = ROW_DP), v = VAlign.CENTER) {
                    +Text(env.string(label), 12f, env.text, TextWeight.MEDIUM, env.font, mod = Mod(width = labelW))
                    +row(Mod(width = trackW, height = 10f, background = track, corner = 5f), v = VAlign.CENTER) {
                        +box(Mod(width = (trackW * fraction).coerceAtLeast(10f), height = 10f, background = env.accent, corner = 5f)) {}
                    }
                    +Text("${(fraction * 100).toInt()} %", 12f, env.text, TextWeight.MEDIUM, env.font, align = TextAlign.END, mod = Mod(width = pctW))
                }
            }
        }
    }

    private const val ROW_DP = 34f
}
