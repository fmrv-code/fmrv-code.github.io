package fmrv.widgetstudio.ui.gallery

import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.TemplateSection
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetLayout
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.model.WidgetTemplate
import fmrv.widgetstudio.ui.editor.StylePresets

/** Catalogue de la galerie. L'identifiant est utilisé dans la navigation. */
object Templates {

    private val base = WidgetConfig(appWidgetId = WidgetConfig.NO_ID)

    val all: List<WidgetTemplate> = listOf(
        WidgetTemplate(
            id = "icon",
            title = R.string.tpl_icon,
            subtitle = R.string.tpl_icon_desc,
            section = TemplateSection.APPS,
            config = StylePresets.Transparent.applyTo(base.copy(contentType = WidgetContentType.APP_SHORTCUT)),
            size = WidgetSizeOption.SMALL,
        ),
        WidgetTemplate(
            id = "pill",
            title = R.string.tpl_pill,
            subtitle = R.string.tpl_pill_desc,
            section = TemplateSection.APPS,
            config = StylePresets.GlassLight.applyTo(
                base.copy(
                    contentType = WidgetContentType.APP_SHORTCUT,
                    layout = WidgetLayout.PILL,
                    shape = WidgetShape.PILL,
                    accentColor = 0xFFE5484D.toInt(),
                ),
            ),
            size = WidgetSizeOption.BAR,
        ),
        WidgetTemplate(
            id = "grid",
            title = R.string.tpl_grid,
            subtitle = R.string.tpl_grid_desc,
            section = TemplateSection.APPS,
            config = StylePresets.GlassLight.applyTo(base.copy(contentType = WidgetContentType.APP_GRID, name = "")),
            size = WidgetSizeOption.MEDIUM,
        ),
        WidgetTemplate(
            id = "search",
            title = R.string.tpl_search,
            subtitle = R.string.tpl_search_desc,
            section = TemplateSection.APPS,
            config = StylePresets.White.applyTo(
                base.copy(contentType = WidgetContentType.SEARCH_BAR, shape = WidgetShape.PILL),
            ),
            size = WidgetSizeOption.BAR,
        ),
        WidgetTemplate(
            id = "clock",
            title = R.string.tpl_clock,
            subtitle = R.string.tpl_clock_desc,
            section = TemplateSection.INFO,
            config = StylePresets.BlueNight.applyTo(base.copy(contentType = WidgetContentType.CLOCK_DATE)),
            size = WidgetSizeOption.MEDIUM,
        ),
        WidgetTemplate(
            id = "news",
            title = R.string.tpl_news,
            subtitle = R.string.tpl_news_desc,
            section = TemplateSection.INFO,
            config = StylePresets.BlueNight.applyTo(base.copy(contentType = WidgetContentType.NEWS_FEED, name = "Franceinfo")),
            size = WidgetSizeOption.WIDE,
        ),
        WidgetTemplate(
            id = "notes",
            title = R.string.tpl_notes,
            subtitle = R.string.tpl_notes_desc,
            section = TemplateSection.INFO,
            config = StylePresets.Coral.applyTo(base.copy(contentType = WidgetContentType.NOTES_LIST, name = "Notes Joplin")),
            size = WidgetSizeOption.WIDE,
        ),
        WidgetTemplate(
            id = "mail",
            title = R.string.tpl_mail,
            subtitle = R.string.tpl_mail_desc,
            section = TemplateSection.INFO,
            config = StylePresets.Lavender.applyTo(base.copy(contentType = WidgetContentType.MAIL_INBOX, name = "Gmail")),
            size = WidgetSizeOption.WIDE,
        ),
        WidgetTemplate(
            id = "device",
            title = R.string.tpl_device,
            subtitle = R.string.tpl_device_desc,
            section = TemplateSection.DEVICE,
            config = StylePresets.GlassLight.applyTo(base.copy(contentType = WidgetContentType.DEVICE_STATUS)),
            size = WidgetSizeOption.MEDIUM,
        ),
    )

    fun byId(id: String?): WidgetTemplate? = all.firstOrNull { it.id == id }
}
