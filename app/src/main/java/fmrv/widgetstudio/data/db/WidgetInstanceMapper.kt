package fmrv.widgetstudio.data.db

import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.model.WidgetStyle
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Un JSON illisible (version future, corruption) redonne le style ou les paramètres par défaut plutôt qu'un plantage. */
fun WidgetInstanceEntity.toDomain(): WidgetInstance = WidgetInstance(
    appWidgetId = appWidgetId,
    kindId = kindId,
    name = name,
    style = try {
        json.decodeFromString<WidgetStyle>(styleJson).let { it.copy(opacity = it.opacity.coerceIn(0f, 1f)) }
    } catch (e: SerializationException) {
        WidgetStyle()
    },
    params = try {
        json.decodeFromString<Params>(paramsJson)
    } catch (e: SerializationException) {
        Params()
    },
)

fun WidgetInstance.toEntity(updatedAt: Long): WidgetInstanceEntity = WidgetInstanceEntity(
    appWidgetId = appWidgetId,
    kindId = kindId,
    name = name,
    styleJson = json.encodeToString(WidgetStyle.serializer(), style),
    paramsJson = json.encodeToString(Params.serializer(), params),
    updatedAt = updatedAt,
)
