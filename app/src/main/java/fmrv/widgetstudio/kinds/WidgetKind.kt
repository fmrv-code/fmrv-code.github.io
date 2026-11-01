package fmrv.widgetstudio.kinds

import android.content.Context
import androidx.annotation.StringRes
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.model.WidgetStyle
import fmrv.widgetstudio.domain.repository.ItemsRepository
import fmrv.widgetstudio.render.Node
import fmrv.widgetstudio.widget.WidgetEntryPoint
import java.time.ZonedDateTime
import java.util.Locale

/** Ce dont un type de widget a besoin pour charger ses données (système, base, réseau). */
class LoadEnv(
    val context: Context,
    val entry: WidgetEntryPoint,
    val instance: WidgetInstance,
)

/** Ce dont un type de widget a besoin pour se dessiner : taille réelle, style, paramètres, heure. */
class RenderEnv(
    val widthDp: Float,
    val heightDp: Float,
    val style: WidgetStyle,
    val params: Params,
    val name: String,
    val now: ZonedDateTime = ZonedDateTime.now(),
    val locale: Locale = Locale.getDefault(),
    val strings: (Int) -> String = { "" },
) {
    val text: Int get() = style.textColor
    val accent: Int get() = style.accentColor
    val font get() = style.font

    fun string(@StringRes id: Int): String = strings(id)

    fun string(@StringRes id: Int, vararg args: Any): String = String.format(locale, strings(id), *args)
}

/** Un type de widget du catalogue : décrit ses réglages, charge ses données et produit son arbre d'éléments. */
interface WidgetKind<D> {
    val id: String
    val category: Category

    @get:StringRes
    val title: Int

    @get:StringRes
    val description: Int

    val defaultSize: WidgetSizeOption
    val defaultStyle: WidgetStyle
    val defaultParams: Map<String, String> get() = emptyMap()

    /** Réglages propres à ce type ; l'éditeur construit son formulaire à partir de cette liste. */
    val fields: List<ParamField> get() = emptyList()

    /** Faux tant qu'il manque un réglage indispensable (application cible, date…). */
    fun isComplete(params: Params): Boolean = true

    suspend fun load(env: LoadEnv): D

    fun render(env: RenderEnv, data: D): Node

    /** Source distante à actualiser en arrière-plan (liste de notes, flux, e-mails), le cas échéant. */
    fun dataSource(entry: WidgetEntryPoint): ItemsRepository? = null
}

/** Un type de widget dont les données sont déjà chargées. */
interface PreparedWidget {
    val kind: WidgetKind<*>

    fun render(env: RenderEnv): Node
}

private class PreparedImpl<D>(override val kind: WidgetKind<D>, private val data: D) : PreparedWidget {
    override fun render(env: RenderEnv): Node = kind.render(env, data)
}

private suspend fun <D> prepareTyped(kind: WidgetKind<D>, env: LoadEnv): PreparedWidget =
    PreparedImpl(kind, kind.load(env))

suspend fun WidgetKind<*>.prepare(env: LoadEnv): PreparedWidget = prepareTyped(this, env)
