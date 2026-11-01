package fmrv.widgetstudio.data.db

import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetLayout
import fmrv.widgetstudio.domain.model.WidgetShape

private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

fun WidgetConfigEntity.toDomain(): WidgetConfig = WidgetConfig(
    appWidgetId = appWidgetId,
    name = name,
    contentType = enumOrDefault(contentType, WidgetContentType.APP_SHORTCUT),
    targetPackage = targetPackage,
    targetLabel = targetLabel,
    shape = enumOrDefault(shape, WidgetShape.ROUNDED),
    background = enumOrDefault(background, BackgroundStyle.SOLID),
    backgroundColor = backgroundColor,
    backgroundEnd = backgroundEnd,
    textColor = textColor,
    accentColor = accentColor,
    opacity = opacity.coerceIn(0f, 1f),
    font = enumOrDefault(font, WidgetFont.SANS),
    layout = enumOrDefault(layout, WidgetLayout.ICON),
    packages = packages.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    label = label,
    showLabel = showLabel,
    use24Hour = use24Hour,
    showDate = showDate,
)

fun WidgetConfig.toEntity(updatedAt: Long): WidgetConfigEntity = WidgetConfigEntity(
    appWidgetId = appWidgetId,
    name = name,
    contentType = contentType.name,
    targetPackage = targetPackage,
    targetLabel = targetLabel,
    shape = shape.name,
    background = background.name,
    backgroundColor = backgroundColor,
    backgroundEnd = backgroundEnd,
    textColor = textColor,
    accentColor = accentColor,
    opacity = opacity,
    font = font.name,
    layout = layout.name,
    packages = packages.joinToString(","),
    label = label,
    showLabel = showLabel,
    use24Hour = use24Hour,
    showDate = showDate,
    updatedAt = updatedAt,
)
