package fmrv.widgetstudio.kinds.apps

import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.StylePresets
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.kinds.LoadEnv
import fmrv.widgetstudio.kinds.ParamField
import fmrv.widgetstudio.kinds.RenderEnv
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.kinds.appLabelKey
import fmrv.widgetstudio.render.Click
import fmrv.widgetstudio.render.Colors
import fmrv.widgetstudio.render.HAlign
import fmrv.widgetstudio.render.Img
import fmrv.widgetstudio.render.ImgSource
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
import fmrv.widgetstudio.render.space

private fun appIcon(pkg: String, size: Float, mod: Mod = Mod()): Node =
    if (pkg.isEmpty()) {
        Img(ImgSource.Res(R.drawable.ic_widget_placeholder), size, size, tint = 0xFFFFFFFF.toInt(), mod = mod)
    } else {
        Img(ImgSource.AppIcon(pkg), size, size, mod = mod)
    }

/** Une application, en un geste. */
object AppIconKind : WidgetKind<Unit> {
    override val id = "app.icon"
    override val category = Category.APPS
    override val title = R.string.kind_app_icon
    override val description = R.string.kind_app_icon_desc
    override val defaultSize = WidgetSizeOption.SMALL
    override val defaultStyle = StylePresets.Transparent.style
    override val defaultParams = mapOf("label" to "1")
    override val fields = listOf(
        ParamField.AppPick("app", R.string.section_app),
        ParamField.Toggle("label", R.string.option_show_label),
    )

    override fun isComplete(params: Params) = params.string("app").isNotEmpty()

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val pkg = env.params.string("app")
        val showLabel = env.params.bool("label", true) && env.heightDp >= 72f
        val iconDp = (minOf(env.widthDp, env.heightDp) * if (showLabel) 0.48f else 0.66f).coerceIn(28f, 72f)
        val click = if (pkg.isEmpty()) Click.Configure else Click.LaunchApp(pkg)
        return column(Mod(fillWidth = true, fillHeight = true, pad = Pad.all(6f), click = click), HAlign.CENTER, VAlign.CENTER) {
            +appIcon(pkg, iconDp)
            if (showLabel) {
                +space(h = 4f)
                +Text(
                    env.params.string(appLabelKey("app")).ifBlank { env.name },
                    (minOf(env.widthDp, env.heightDp) * 0.11f).coerceIn(10f, 14f),
                    env.text, TextWeight.MEDIUM, env.font, TextAlign.CENTER,
                    mod = Mod(fillWidth = true),
                )
            }
        }
    }
}

/** Grande pastille ronde et libellé, dans l'esprit des widgets de dictaphone ou de minuteur. */
object AppPillKind : WidgetKind<Unit> {
    override val id = "app.pill"
    override val category = Category.APPS
    override val title = R.string.kind_app_pill
    override val description = R.string.kind_app_pill_desc
    override val defaultSize = WidgetSizeOption.BAR
    override val defaultStyle = StylePresets.GlassLight.style.copy(
        accentColor = 0xFFE5484D.toInt(),
        shape = fmrv.widgetstudio.domain.model.WidgetShape.PILL,
    )
    override val fields = listOf(
        ParamField.AppPick("app", R.string.section_app),
        ParamField.Text("text", R.string.section_label_pill, R.string.pill_hint),
    )

    override fun isComplete(params: Params) = params.string("app").isNotEmpty()

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val pkg = env.params.string("app")
        val d = (env.heightDp - 16f).coerceIn(28f, 64f)
        val label = env.params.string("text").ifBlank { env.params.string(appLabelKey("app")).ifBlank { env.name } }
        val click = if (pkg.isEmpty()) Click.Configure else Click.LaunchApp(pkg)
        return row(Mod(fillWidth = true, fillHeight = true, pad = Pad.sym(horizontal = 8f), click = click), v = VAlign.CENTER) {
            +box(Mod(width = d, height = d, background = env.accent, corner = d / 2f)) {
                +appIcon(pkg, d * 0.62f)
            }
            +space(w = 12f)
            +Text(label, (d * 0.42f).coerceIn(14f, 28f), env.text, TextWeight.BOLD, env.font, mod = Mod(weight = 1f))
        }
    }
}

/** Dossier : jusqu'à huit applications en grille. */
object AppFolderKind : WidgetKind<Unit> {
    override val id = "app.folder"
    override val category = Category.APPS
    override val title = R.string.kind_app_folder
    override val description = R.string.kind_app_folder_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.GlassLight.style
    override val defaultParams = mapOf("showTitle" to "1")
    override val fields = listOf(
        ParamField.AppList("apps", R.string.section_apps_folder, WidgetInstance.MAX_FOLDER_APPS),
        ParamField.Text("title", R.string.field_folder_title),
        ParamField.Toggle("showTitle", R.string.option_show_label),
    )

    override fun isComplete(params: Params) = params.list("apps").isNotEmpty()

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val apps = env.params.list("apps").take(WidgetInstance.MAX_FOLDER_APPS)
        val title = env.params.string("title").ifBlank { env.name }
        val showTitle = env.params.bool("showTitle", true) && env.heightDp >= 140f && title.isNotBlank()
        val columns = if (env.widthDp >= 200f) 4 else 3
        val rows = if (apps.isEmpty()) 0 else (apps.size + columns - 1) / columns
        val cell = (env.widthDp - 20f) / columns
        val cellH = (env.heightDp - 20f - if (showTitle) 18f else 0f) / rows.coerceAtLeast(1)
        val icon = (minOf(cell, cellH) * 0.74f).coerceIn(24f, 52f)

        return column(Mod.Fill.copy(pad = Pad.all(10f)), HAlign.CENTER, VAlign.CENTER) {
            if (apps.isEmpty()) {
                +Text(env.string(R.string.folder_empty), 13f, env.text, font = env.font, align = TextAlign.CENTER, maxLines = 2)
            }
            apps.chunked(columns).forEach { rowApps ->
                +row(Mod(fillWidth = true, pad = Pad.sym(vertical = 3f))) {
                    rowApps.forEach { pkg ->
                        +box(Mod(width = cell, click = Click.LaunchApp(pkg))) { +appIcon(pkg, icon) }
                    }
                }
            }
            if (showTitle) {
                +space(h = 2f)
                +Text(title, 12f, env.text, TextWeight.MEDIUM, env.font, TextAlign.CENTER)
            }
        }
    }
}

/** Barre de recherche : ouvre le navigateur ou le moteur choisi. */
object SearchBarKind : WidgetKind<Unit> {
    override val id = "app.search"
    override val category = Category.APPS
    override val title = R.string.kind_app_search
    override val description = R.string.kind_app_search_desc
    override val defaultSize = WidgetSizeOption.BAR
    override val defaultStyle = StylePresets.White.style.copy(shape = fmrv.widgetstudio.domain.model.WidgetShape.PILL)
    override val fields = listOf(
        ParamField.AppPick("app", R.string.section_app),
        ParamField.Text("hint", R.string.section_label_search, R.string.search_hint),
    )

    override fun isComplete(params: Params) = params.string("app").isNotEmpty()

    override suspend fun load(env: LoadEnv) = Unit

    override fun render(env: RenderEnv, data: Unit): Node {
        val pkg = env.params.string("app")
        val d = (env.heightDp - 16f).coerceIn(28f, 64f)
        val hint = env.params.string("hint").ifBlank { env.string(R.string.search_hint_default) }
        val click = if (pkg.isEmpty()) Click.Configure else Click.LaunchApp(pkg)
        return row(Mod(fillWidth = true, fillHeight = true, pad = Pad.sym(horizontal = 8f), click = click), v = VAlign.CENTER) {
            +appIcon(pkg, d * 0.82f)
            +space(w = 12f)
            +Text(hint, (d * 0.42f).coerceIn(14f, 28f), Colors.withOpacity(env.text, 0.78f), font = env.font, mod = Mod(weight = 1f))
            +Img(ImgSource.Res(R.drawable.ic_search), 24f, 24f, tint = env.text)
            +space(w = 6f)
        }
    }
}
