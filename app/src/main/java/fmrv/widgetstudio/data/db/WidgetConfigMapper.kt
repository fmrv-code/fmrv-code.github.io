package fmrv.widgetstudio.data.db

import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
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
    backgroundColor = backgroundColor,
    textColor = textColor,
    opacity = opacity.coerceIn(0f, 1f),
    font = enumOrDefault(font, WidgetFont.SANS),
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
    backgroundColor = backgroundColor,
    textColor = textColor,
    opacity = opacity,
    font = font.name,
    showLabel = showLabel,
    use24Hour = use24Hour,
    showDate = showDate,
    updatedAt = updatedAt,
)
